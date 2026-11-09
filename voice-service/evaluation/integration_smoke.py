"""Smoke-test Spring -> Python against a temporary copy of the local catalog DB.

Requires local MySQL, mysql CLI, Gradle, and installed voice-service/.venv.
The temporary database and launched servers are removed in finally.
"""

import base64
import argparse
import hashlib
import hmac
import json
import os
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
VOICE = ROOT / "voice-service"
PROPERTIES = dict(line.split("=", 1) for line in (ROOT / "src/main/resources/application.properties")
                  .read_text().splitlines() if "=" in line and not line.startswith("#"))
SOURCE_DB = "misterworld"
DB = "misterworld_voice_verify_" + uuid.uuid4().hex[:10]
PYTHON_PORT = 18001
SPRING_PORT = 18080
MYSQL_ENV = {**os.environ, "MYSQL_PWD": PROPERTIES["spring.datasource.password"]}


def sql(statement: str) -> str:
    result = subprocess.run(["mysql", "-h", "127.0.0.1", "-u", PROPERTIES["spring.datasource.username"],
                             "-N", "-B", "-e", statement], env=MYSQL_ENV, capture_output=True, text=True, check=True)
    return result.stdout.strip()


def http(path: str, *, payload: dict | None = None, token: str | None = None,
         multipart: bytes | None = None) -> tuple[int, dict]:
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    if multipart is not None:
        headers["Content-Type"] = "multipart/form-data; boundary=voice-check"
        data = multipart
    elif payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload, ensure_ascii=False).encode()
    else:
        data = None
    request = urllib.request.Request(f"http://127.0.0.1:{SPRING_PORT}{path}", data=data, headers=headers)
    try:
        response = urllib.request.urlopen(request, timeout=190)
    except urllib.error.HTTPError as exc:
        response = exc
    with response:
        raw = response.read()
        return response.status, json.loads(raw) if raw else {}


def token(role: str) -> str:
    def encoded(value: dict) -> str:
        return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).rstrip(b"=").decode()
    now = int(time.time())
    unsigned = encoded({"alg": "HS256", "typ": "JWT"}) + "." + encoded(
        {"sub": "1", "email": "voice-test@example.invalid", "role": role, "type": "access", "iat": now, "exp": now + 3600})
    key = base64.b64decode(PROPERTIES["jwt.secret"])
    signature = base64.urlsafe_b64encode(hmac.new(key, unsigned.encode(), hashlib.sha256).digest()).rstrip(b"=").decode()
    return unsigned + "." + signature


def wait_for(port: int, process: subprocess.Popen, seconds: int) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"Server on port {port} exited with {process.returncode}")
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=0.5):
                return
        except OSError:
            time.sleep(0.5)
    raise TimeoutError(f"Server on port {port} did not start")


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify Spring and Python voice APIs with an isolated local DB")
    parser.add_argument("--audio", type=Path, help="Optional real WAV recording to test successful transcription")
    args = parser.parse_args()
    servers = []
    created = False
    with tempfile.TemporaryDirectory(prefix="misterworld_voice_smoke_") as temp:
        try:
            sql(f"CREATE DATABASE `{DB}`")
            created = True
            for table in ("여행상품", "여행일정", "여행옵션"):
                sql(f"CREATE TABLE `{DB}`.`{table}` LIKE `{SOURCE_DB}`.`{table}`")
                sql(f"INSERT INTO `{DB}`.`{table}` SELECT * FROM `{SOURCE_DB}`.`{table}`")
            sql(f"ALTER TABLE `{DB}`.`여행일정` MODIFY COLUMN `여행상태` VARCHAR(30) NULL")
            sql(f"UPDATE `{DB}`.`여행일정` SET `여행상태`=CONCAT(`여행상태`, '_SCHEDULE') "
                "WHERE `여행상태` IN ('RECRUITING','CLOSED','IN_PROGRESS','COMPLETED','CANCELLED')")

            python_log = open(Path(temp) / "python.log", "w")
            spring_log = open(Path(temp) / "spring.log", "w")
            python = subprocess.Popen([str(VOICE / ".venv/bin/python"), "-m", "uvicorn", "app.main:app",
                                       "--host", "127.0.0.1", "--port", str(PYTHON_PORT)],
                                      cwd=VOICE, stdout=python_log, stderr=subprocess.STDOUT)
            servers.append((python, python_log))
            wait_for(PYTHON_PORT, python, 45)
            spring = subprocess.Popen([str(ROOT / "gradlew"), "bootRun", "--offline",
                                       f"--args=--server.port={SPRING_PORT} --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/{DB} "
                                       "--spring.jpa.hibernate.ddl-auto=update --spring.jpa.show-sql=false "
                                       f"--voice.python.base-url=http://127.0.0.1:{PYTHON_PORT}"],
                                      cwd=ROOT, stdout=spring_log, stderr=subprocess.STDOUT)
            servers.append((spring, spring_log))
            wait_for(SPRING_PORT, spring, 90)
            before = sql(f"SELECT COUNT(*) FROM `{DB}`.`여행신청`")

            request = {"text": "푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘",
                       "context": {"productId": None, "scheduleId": None, "grade": None,
                                   "optionIds": {"HOTEL": None, "TRANSPORT": None, "MEAL": None, "EXTRA": []}}}
            status, response = http("/api/v1/voice/interpretations", payload=request, token=token("MEMBER"))
            assert status == 200, (status, response)
            assert response["fields"]["tour"]["status"] == "MATCHED", response
            assert response["fields"]["schedule"]["status"] == "MATCHED", response
            assert response["fields"]["grade"]["status"] == "MATCHED", response
            assert response["fields"]["hotel"]["status"] == "MATCHED", response
            assert response["nextAction"] == "REVIEW" and response["failureCount"] == 0, response
            print("PASS Spring -> Python -> catalog interpretation", flush=True)

            bad = {**request, "context": {**request["context"], "productId": response["selection"]["productId"],
                                               "scheduleId": 999999999}}
            status, _ = http("/api/v1/voice/interpretations", payload=bad, token=token("MEMBER"))
            assert status == 400, status
            print("PASS invalid current selection rejected", flush=True)

            for role in (None, "STAFF"):
                status, _ = http("/api/v1/voice/interpretations", payload=request,
                                 token=token(role) if role else None)
                assert status in (401, 403), status
            print("PASS unauthenticated and staff access rejected", flush=True)

            invalid_audio = (b"--voice-check\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"bad.wav\"\r\n"
                             b"Content-Type: audio/wav\r\n\r\nnot a wav\r\n--voice-check--\r\n")
            status, _ = http("/api/v1/voice/transcriptions", multipart=invalid_audio, token=token("MEMBER"))
            assert status == 415, status
            print("PASS invalid audio rejected through both servers", flush=True)

            if args.audio:
                audio_bytes = args.audio.read_bytes()
                body = (b"--voice-check\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"sample.wav\"\r\n"
                        b"Content-Type: audio/wav\r\n\r\n" + audio_bytes + b"\r\n--voice-check--\r\n")
                status, transcript = http("/api/v1/voice/transcriptions", multipart=body, token=token("MEMBER"))
                assert status == 200 and transcript.get("text"), (status, transcript)
                print("PASS successful transcription through both servers", flush=True)

            after = sql(f"SELECT COUNT(*) FROM `{DB}`.`여행신청`")
            assert before == after, (before, after)
            print("PASS interpretation created no application", flush=True)
        except Exception:
            for name in ("python.log", "spring.log"):
                path = Path(temp) / name
                if path.exists():
                    print(f"--- {name} (last 25 lines) ---", flush=True)
                    print("\n".join(path.read_text(errors="replace").splitlines()[-25:]), flush=True)
            raise
        finally:
            for process, log in reversed(servers):
                process.terminate()
                try:
                    process.wait(timeout=8)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=8)
                log.close()
            if created:
                sql(f"DROP DATABASE `{DB}`")


if __name__ == "__main__":
    main()

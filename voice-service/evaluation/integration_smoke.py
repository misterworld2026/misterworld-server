"""Smoke-test Spring -> Python against a temporary empty local MySQL DB.

Requires local MySQL, mysql CLI, Gradle, and installed voice-service/.venv.
The temporary database and launched servers are removed in finally.
"""

import argparse
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
DB = "misterworld_voice_verify_" + uuid.uuid4().hex[:10]
PYTHON_PORT = 18001
SPRING_PORT = 18080
MYSQL_USER = os.getenv("VOICE_TEST_DB_USER") or os.getenv("DB_USERNAME") or "root"
MYSQL_PASSWORD = os.getenv("VOICE_TEST_DB_PASSWORD") or os.getenv("DB_PASSWORD")
if MYSQL_PASSWORD is None:
    local_properties = ROOT / "src/main/resources/application.properties"
    if local_properties.exists():
        local_settings = dict(line.split("=", 1) for line in local_properties.read_text().splitlines()
                              if "=" in line and not line.startswith("#"))
        MYSQL_PASSWORD = local_settings.get("spring.datasource.password", "")
MYSQL_ENV = {**os.environ, "MYSQL_PWD": MYSQL_PASSWORD or ""}


def sql(statement: str) -> str:
    result = subprocess.run(["mysql", "-h", "127.0.0.1", "-u", MYSQL_USER,
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

            python_log = open(Path(temp) / "python.log", "w")
            spring_log = open(Path(temp) / "spring.log", "w")
            python = subprocess.Popen([str(VOICE / ".venv/bin/python"), "-m", "uvicorn", "app.main:app",
                                       "--host", "127.0.0.1", "--port", str(PYTHON_PORT)],
                                      cwd=VOICE, stdout=python_log, stderr=subprocess.STDOUT)
            servers.append((python, python_log))
            wait_for(PYTHON_PORT, python, 45)
            spring_env = {**os.environ,
                          "SPRING_DATASOURCE_URL": f"jdbc:mysql://127.0.0.1:3306/{DB}",
                          "SPRING_DATASOURCE_USERNAME": MYSQL_USER,
                          "SPRING_DATASOURCE_PASSWORD": MYSQL_PASSWORD or "",
                          "SPRING_DATASOURCE_DRIVER_CLASS_NAME": "com.mysql.cj.jdbc.Driver",
                          "SPRING_JPA_HIBERNATE_DDL_AUTO": "validate",
                          "APP_JWT_SECRET": "voice-integration-local-only-secret-at-least-32-bytes",
                          "VOICE_PYTHON_BASE_URL": f"http://127.0.0.1:{PYTHON_PORT}"}
            spring = subprocess.Popen([str(ROOT / "gradlew"), "bootRun", "--offline",
                                       f"--args=--server.port={SPRING_PORT}"],
                                      cwd=ROOT, env=spring_env, stdout=spring_log, stderr=subprocess.STDOUT)
            servers.append((spring, spring_log))
            wait_for(SPRING_PORT, spring, 90)
            sql(f"INSERT INTO `{DB}`.`여행상품` (`상품명`,`테마`,`상품설명`,`클래식_가격`,`그랜드_가격`,`프리미엄_가격`) "
                "VALUES ('푸껫 허니문 5박 7일','허니문','음성 통합 검증',NULL,2190000,2790000)")
            product_id = int(sql(f"SELECT `여행상품_ID` FROM `{DB}`.`여행상품` WHERE `상품명`='푸껫 허니문 5박 7일'"))
            sql(f"INSERT INTO `{DB}`.`여행일정` (`여행상품_ID`,`출발일`,`종료일`,`여행상태`,`현재신청인원`) "
                f"VALUES ({product_id},'2027-03-01','2027-03-07','RECRUITING_SCHEDULE',0)")
            sql(f"INSERT INTO `{DB}`.`여행옵션` (`여행상품_ID`,`옵션종류`,`옵션명`,`옵션등급`,`추가금액`) "
                f"VALUES ({product_id},'HOTEL','오션뷰 객실 업그레이드','GRAND',180000)")
            email = f"voice-test-{uuid.uuid4().hex[:8]}@example.invalid"
            signup = {"email": email, "password": "test-password-123", "passwordConfirmation": "test-password-123",
                      "name": "음성 테스트", "address": "서울", "phoneNumber": "01012345678"}
            status, response = http("/api/auth/signup", payload=signup)
            assert status == 201, (status, response)
            status, response = http("/api/auth/login", payload={"email": email, "password": signup["password"]})
            assert status == 200, (status, response)
            member_token = response["accessToken"]
            before = sql(f"SELECT COUNT(*) FROM `{DB}`.`여행신청`")

            request = {"text": "푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘",
                       "context": {"productId": None, "scheduleId": None, "grade": None,
                                   "optionIds": {"HOTEL": None, "TRANSPORT": None, "MEAL": None, "EXTRA": []}}}
            status, response = http("/api/v1/voice/interpretations", payload=request, token=member_token)
            assert status == 200, (status, response)
            assert response["fields"]["tour"]["status"] == "MATCHED", response
            assert response["fields"]["schedule"]["status"] == "MATCHED", response
            assert response["fields"]["grade"]["status"] == "MATCHED", response
            assert response["fields"]["hotel"]["status"] == "MATCHED", response
            assert response["nextAction"] == "REVIEW" and response["failureCount"] == 0, response
            print("PASS Spring -> Python -> catalog interpretation", flush=True)

            bad = {**request, "context": {**request["context"], "productId": response["selection"]["productId"],
                                               "scheduleId": 999999999}}
            status, _ = http("/api/v1/voice/interpretations", payload=bad, token=member_token)
            assert status == 400, status
            print("PASS invalid current selection rejected", flush=True)

            status, _ = http("/api/v1/voice/interpretations", payload=request)
            assert status == 401, status
            sql(f"UPDATE `{DB}`.`회원` SET `회원구분`='STAFF' WHERE `이메일`='{email}'")
            status, _ = http("/api/v1/voice/interpretations", payload=request, token=member_token)
            assert status == 403, status
            sql(f"UPDATE `{DB}`.`회원` SET `회원구분`='MEMBER' WHERE `이메일`='{email}'")
            print("PASS unauthenticated and staff access rejected", flush=True)

            invalid_audio = (b"--voice-check\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"bad.wav\"\r\n"
                             b"Content-Type: audio/wav\r\n\r\nnot a wav\r\n--voice-check--\r\n")
            status, _ = http("/api/v1/voice/transcriptions", multipart=invalid_audio, token=member_token)
            assert status == 415, status
            print("PASS invalid audio rejected through both servers", flush=True)

            if args.audio:
                audio_bytes = args.audio.read_bytes()
                body = (b"--voice-check\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"sample.wav\"\r\n"
                        b"Content-Type: audio/wav\r\n\r\n" + audio_bytes + b"\r\n--voice-check--\r\n")
                status, transcript = http("/api/v1/voice/transcriptions", multipart=body, token=member_token)
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

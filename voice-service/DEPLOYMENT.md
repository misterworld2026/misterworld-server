# 시연 배포: EC2 Spring + 로컬 Python

시연에서는 Spring과 MySQL을 기존 EC2에서 실행하고, Whisper small을 개발자 노트북의 Python 서버에서 실행한다. 문장 해석은 노트북에 `GROQ_API_KEY`가 설정되어 있으면 Groq API를 사용하고, 없으면 로컬 Qwen3-1.7B를 사용한다. EC2의 4 GiB 메모리에는 AI 모델을 올리지 않는다. `.github/workflows/deploy.yml`은 백엔드만 빌드하고 배포한다. Compose의 `voice-ai` 서비스는 `local-ai` 프로필을 지정했을 때만 실행하므로 EC2 기본 배포에서 시작되지 않는다.

Spring 컨테이너에서 노트북의 `localhost`는 보이지 않는다. 시연 전에 EC2에서 접근 가능한 **비공개 네트워크 주소**를 준비해야 한다. 아래는 [Tailscale Serve](https://tailscale.com/docs/reference/tailscale-cli/serve)를 사용하는 예시다. 두 기기가 같은 tailnet에 로그인하고, EC2에서 노트북의 서비스에 접근하도록 tailnet 접근 정책을 확인한다. 공개 인터넷에 Python의 `/internal/v1/...`를 노출하지 않는다.

## 개발자 노트북

1. Tailscale에 로그인한다.
2. Groq 해석을 사용할 때는 저장소의 `voice-service`에서 `GROQ_API_KEY`를 환경 변수로 설정한다. 키를 저장소나 EC2에 복사하지 않는다. 같은 터미널에서 Python 서버를 실행한다.

   ```bash
   .venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
   ```

3. 다른 터미널에서 `tailscale serve --http=8001 8001`로 로컬 8001 포트를 tailnet에 공유한다. `tailscale serve status`와 `tailscale ip -4`로 상태와 노트북의 tailnet IP를 확인한다. 시연하는 동안 노트북, Python 서버, Tailscale 연결을 유지한다.

## EC2 담당자

1. EC2도 같은 tailnet에 연결한다. EC2 호스트에서 `curl http://<노트북-tailnet-IP>:8001/health`가 `{"status":"ok"}`를 반환하는지 확인한다.
2. EC2의 `~/misterworld-server/.env`에 `VOICE_DOCKER_PYTHON_BASE_URL=http://<노트북-tailnet-IP>:8001`을 추가한다. `COMPOSE_FILE=docker-compose.yml:compose.production.yaml`은 유지한다. 비밀번호나 실제 주소를 저장소에 커밋하지 않는다.
3. 백엔드 배포 후 Spring 컨테이너에서도 해당 주소에 접근 가능한지 확인한다. 호스트에서는 연결되지만 컨테이너에서는 연결되지 않으면 Docker와 Tailscale 사이의 라우팅을 별도로 점검한다.
4. 정상 회원으로 `/api/v1/voice/transcriptions`와 `/api/v1/voice/interpretations`를 호출해 실제 전사와 상품·옵션 미리보기를 확인한다. `GET /health`는 웹 서버 상태만 뜻하며 모델 추론 성공을 보증하지 않는다.

노트북이 꺼지거나 사설 연결이 끊기면 Spring의 음성 API는 실패한다. 이 구성은 시연용이다. 24시간 운영하려면 충분한 메모리를 갖춘 별도 AI 실행 환경을 정해야 한다.

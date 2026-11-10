# EC2 음성 서비스 배포

음성 처리부는 Spring 백엔드와 같은 EC2의 별도 Docker Compose 서비스 `voice-ai`로 실행한다. 외부 포트는 열지 않는다. Spring 컨테이너는 내부 네트워크에서 `http://voice-ai:8001`로 호출한다.

## 저장소에 준비된 설정

- `voice-service/Dockerfile`: Python 3.13, CPU용 PyTorch, 음성 서비스 의존성 설치 및 Uvicorn 실행.
- `voice-service/.dockerignore`: 로컬 가상환경, 모델, 평가 자료를 빌드에서 제외.
- `docker-compose.yml`: `voice-ai` 서비스, `/app/models` 영속 볼륨, 상태 검사, Spring의 `VOICE_PYTHON_BASE_URL` 설정.
- `.github/workflows/deploy.yml`: `main` 푸시 시 백엔드와 음성 이미지를 먼저 빌드한 후 MySQL, 음성 서비스, 백엔드 순서로 실행.

배포 작업은 `voice-ai` 상태 검사가 통과할 때까지 최대 120초 대기한 뒤 백엔드를 교체한다. EC2의 Docker Compose 버전에서 `up --wait`를 지원하는지 확인한다.

모델은 첫 요청 때 내려받아 Compose의 `voice_models` 볼륨에 보관한다. 음성 서비스 컨테이너를 다시 만들더라도 모델을 매번 다시 내려받지 않도록 하기 위함이다. `GET /health`는 웹 서버의 가동 여부만 확인하며 모델 로딩이나 실제 추론 성공을 보증하지 않는다.

## EC2 담당자 확인 항목

1. `free -h`, `df -h`, `uname -m`, `docker compose version`으로 메모리, 디스크, CPU 아키텍처와 Compose 버전을 확인한다. Qwen3-1.7B와 faster-whisper small을 Spring, MySQL과 같은 인스턴스에서 실행할 여유가 있어야 한다. 현재 인스턴스 크기를 모르므로 충분한지 로컬에서 판단할 수 없다. 모델 다운로드에 필요한 외부 네트워크도 확인한다.
2. `main`에 변경사항을 합치기 전에 기존 운영 DB에 현재 Flyway 마이그레이션을 적용해도 되는지 별도로 확인한다. 현재 V1은 빈 DB를 전제로 한다. 이 확인은 AI 서비스 추가와 독립적인 기존 백엔드 배포 조건이다.
3. 머지 후 GitHub Actions의 `Deploy to AWS EC2` 성공 여부를 확인한다. 실패했다면 기존 백엔드와 DB 상태를 먼저 확인하고 빌드 및 컨테이너 로그를 검토한다.
4. EC2에서 `cd ~/misterworld-server && docker compose ps`로 `voice-ai`가 `healthy`, `backend`가 실행 중인지 확인한다. `docker compose logs --tail=100 voice-ai`에서 의존성 오류나 모델 다운로드 오류를 확인한다.
5. EC2에서 `docker compose exec -T voice-ai python -c "import urllib.request; print(urllib.request.urlopen('http://127.0.0.1:8001/health').read().decode())"`로 내부 헬스 응답을 확인한다. 이어서 실제 회원 계정과 테스트 상품·일정·옵션으로 Spring의 `/api/v1/voice/interpretations`를 호출해 모델 추론까지 확인한다.

실제 녹음은 최대 30초·10 MiB이며, 공개 API는 Spring의 `/api/v1/voice/transcriptions`다. Python의 `/internal/v1/...`는 외부에 공개하지 않는다. 첫 요청은 모델 다운로드와 로딩으로 오래 걸릴 수 있다. 메모리 부족이나 시간 초과가 발생하면 인스턴스 자원과 `.env`의 `VOICE_PYTHON_READ_TIMEOUT_MS`를 확인한다.

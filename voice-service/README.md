# 음성 기반 여행상품 선택: 개발 기준

이 문서는 음성 기능을 구현할 때 팀이 함께 지킬 기준을 정리한다. 근거는 프로젝트 계획서의 기술 명세·평가 항목과 설계 산출물 1.1의 3.2, 3.3, 3.4, 3.7, 4.3절이다. 구현 중 설계가 바뀌면 관련 문서와 이 파일을 함께 갱신한다.

## 목표와 역할

- 고객이 음성으로 여행상품과 선택 조건을 말하면 인식한 문장과 해석 결과를 화면에 보여준다.
- 고객은 결과를 확인하거나 수정·재입력한 뒤 일반 여행 신청 화면으로 진행한다.
- 음성 전사는 faster-whisper, 문장 해석은 Qwen3로 구현한다. 음성 처리부는 Python과 FastAPI를 사용한다.
- 프론트엔드는 음성 입력 및 인식·선택 결과의 확인·수정 화면을 담당한다.
- Python 음성 처리부는 음성을 텍스트로 변환하고 문장에서 여행 선택 조건을 해석한다.
- Spring Boot의 `VoiceController`·`VoiceService`는 해석 결과를 실제 상품·옵션·등록 일정과 대조하고 화면에 보여줄 선택 결과를 반환한다.

## 반드시 지킬 규칙

1. 음성 처리 결과만으로 `TravelApplication`을 생성하거나 결제·가격을 확정하지 않는다. 고객이 확인한 선택도 일반 GUI 선택과 같은 `TravelApplicationService`의 검증·신청 절차를 거친다.
2. Python이 만든 상품 ID, 일정 ID, 옵션 ID 또는 가격을 사실로 신뢰하지 않는다. 실제 존재 여부, 신청 가능 일정, 허용 등급·옵션 등 업무 규칙은 Spring 서버가 등록된 데이터를 기준으로 검증한다.
3. 고객이 **언급하지 않은 항목**과 **언급했지만 해석에 실패한 항목**을 구분한다. 언급하지 않은 항목은 추론 실패 횟수에 포함하지 않는다.
4. 설계 산출물의 실패 판단 대상은 **투어, 일정, 호텔, 식사**다. 이 중 언급한 항목에서 두 개 이상 추론에 실패하면 음성 재입력 또는 화면 조작을 안내한다. 실패 항목이 두 개 미만이어도 미해결 항목은 고객에게 확인·수정 가능하게 표시한다.
5. 인식 문장과 해석한 선택 결과를 고객에게 보여주고, 고객의 확인 또는 수정 기회를 제공한다. 자동으로 여행 신청을 완료하지 않는다.
6. 음성 처리부 호출이나 해석이 실패해도 고객이 재시도하거나 GUI로 선택할 수 있어야 한다.

## 연동 시 지켜야 할 데이터 경계

| 구간 | 전달·반환할 내용 | 책임 |
| --- | --- | --- |
| 음성 입력 → STT | 녹음된 음성 → 인식한 문장 | Python 음성 처리부 |
| 문장 → 명령 해석 | 인식 문장, 현재 선택 정보, 선택 가능한 항목·예시 → 해석한 조건과 미해결 항목 | Python 음성 처리부 |
| 명령 해석 → 상품 선택 | 해석한 조건 → 실제 상품·옵션·일정과 대조한 선택 미리보기 | Spring Boot |
| 선택 미리보기 → 신청 | 고객이 확인·수정한 선택 정보 | 기존 여행 신청 기능 |

프롬프트에는 선택 가능한 항목 목록과 예시를 제공하는 few-shot 방식을 사용한다. 해석 결과에 없는 항목을 임의로 채우거나 실제 DB에 없는 선택지를 확정된 값처럼 반환하지 않는다.

## 연동 계약

API 경로, 요청·응답 JSON, 상태값, 오류 처리는 [CONTRACT.md](CONTRACT.md)를 따른다. Spring 백엔드 담당자는 [SPRING_INTEGRATION.md](SPRING_INTEGRATION.md)의 구현 순서와 검증 규칙도 확인한다. 설계 산출물 4.3의 `interpretVoice(text, context)`는 전사한 문장을 고객이 확인한 뒤 호출하는 해석 API로 연결한다.

음성 명령 성공률 측정용 발화 목록과 판정 기준은 통합 검증 단계에서 작성한다. 계획서의 개발 목표치는 **80%**다.

## 로컬 실행

```bash
cd voice-service
python3.13 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

`GET http://127.0.0.1:8001/health`가 `{"status":"ok"}`를 반환하면 서버가 실행 중이다. API 문서는 `http://127.0.0.1:8001/docs`에서 볼 수 있다.

`POST /internal/v1/transcribe`는 `audio` 파일을 받아 한국어 문장을 반환한다. 지원 형식과 제한은 [CONTRACT.md](CONTRACT.md)를 따른다. 첫 전사 요청에서 Whisper 모델을 다운로드하므로 인터넷 연결이 필요하며, 모델 파일은 Git에서 제외되는 `voice-service/models/`에 저장된다. 기본 모델은 다국어 `small`을 CPU에서 실행한다. 필요하면 `VOICE_STT_MODEL`, `VOICE_STT_DEVICE`, `VOICE_STT_COMPUTE_TYPE`, `VOICE_STT_HOTWORDS` 환경 변수로 변경할 수 있다. 기본 힌트 단어는 허니문·효도 여행·골프 여행·트레킹 여행이다. 이는 인식 보조용이며 실제 상품 검증은 Spring이 수행한다.

서버를 켠 뒤 로컬 음성 파일로 전사를 확인할 수 있다.

```bash
curl -F "audio=@sample.wav;type=audio/wav" http://127.0.0.1:8001/internal/v1/transcribe
```

`POST /internal/v1/interpret`는 전사하거나 고객이 수정한 문장, 현재 선택 정보, Spring이 조회한 후보를 받아 항목별 해석 결과를 반환한다. 기본 모델은 `Qwen/Qwen3-1.7B`이며 첫 요청에서 모델 파일을 내려받아 `voice-service/models/`에 저장한다. 모델 로딩과 첫 추론에 시간이 걸릴 수 있다. `VOICE_QWEN_MODEL`로 모델을, `VOICE_QWEN_DEVICE`로 실행 장치(`auto`, `cuda`, `mps`, `cpu`)를 지정할 수 있다. `auto`는 사용 가능한 GPU를 우선한다. 메모리나 처리 속도가 부족한 환경에서는 더 작은 모델을 지정할 수 있지만, 해석 품질은 별도로 확인해야 한다.

```bash
curl -X POST http://127.0.0.1:8001/internal/v1/interpret \
  -H 'Content-Type: application/json' \
  -d '{"text":"푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘","context":{},"candidates":{"products":[],"schedules":[],"options":[]}}'
```

해석 결과는 발화에 나타난 표현만 포함하며 상품·일정·옵션 ID와 가격은 반환하지 않는다. 모델이 만든 값은 원문과 대조한 뒤 응답하고, 실제 선택지와의 일치 여부는 Spring에서 검증한다.

Spring의 공개 API는 인증된 일반 회원이 호출하는 `POST /api/v1/voice/transcriptions`와 `POST /api/v1/voice/interpretations`다. Spring이 Python 서비스에 요청을 보내고 등록된 상품·일정·옵션과 대조한 선택 미리보기를 반환한다. Python 서버를 먼저 실행한 뒤 Spring 서버를 시작한다. 자세한 요청·응답은 [CONTRACT.md](CONTRACT.md), DB 검증과 설정은 [SPRING_INTEGRATION.md](SPRING_INTEGRATION.md)를 따른다.

# Spring 백엔드 음성 기능 연동 가이드

Spring 담당자가 음성 선택 기능을 구현할 때 읽는 문서다. 요청·응답의 정확한 경로와 JSON 예시는 [CONTRACT.md](CONTRACT.md)를 기준으로 한다. 이 문서는 **Spring 쪽 책임, 구현 순서, 검증 규칙, 현재 코드의 통합 전제**를 정리한다.

## 현재 상태와 실행 전 확인

- `feat/voice-selection`에는 상품·인증 코드와 Spring 음성 연동 코드가 포함되어 있다.
- `VoiceController`, `VoiceService`, `VoiceProcessingClient`가 계약의 공개 API 두 개를 제공한다. Python FastAPI의 `GET /health`, `/internal/v1/transcribe`, `/internal/v1/interpret`가 구현되어 있다.
- Spring은 `VOICE_PYTHON_BASE_URL`(기본 `http://127.0.0.1:8001`), `VOICE_PYTHON_CONNECT_TIMEOUT_MS`(기본 3000), `VOICE_PYTHON_READ_TIMEOUT_MS`(기본 180000)로 Python 연결을 설정한다. 모델 첫 로딩·추론 시간을 고려해 실제 환경에서 값을 조정한다.
- `TravelProduct`의 ID는 `id`, `TravelSchedule`의 ID는 `travelScheduleId`, 종료일 필드명은 `arrivalDate`, `TravelOption`의 ID는 `id`다. API 계약의 `id`·`departureDate`로 변환할 때 필드명을 확인한다.
- 개발용 `dev-seed.sql`의 신규 일정 상태는 `RECRUITING_SCHEDULE`로 맞췄다. 기존 DB에 `RECRUITING` 값이 이미 있으면 다음 SQL을 실행해 갱신해야 한다: `UPDATE 여행일정 SET 여행상태 = 'RECRUITING_SCHEDULE' WHERE 여행상태 = 'RECRUITING';`
- 개발용 `dev-seed.sql` 주석은 옵션의 `grade = null`을 해당 상품의 모든 판매 등급에서 사용 가능한 값으로 설명한다. 실제 상품·옵션 검증 규칙에서도 이 의미를 사용할지 담당자와 맞춘 뒤 동일하게 적용한다.

## Spring 구성요소

| 구성요소 | 해야 할 일 |
| --- | --- |
| `VoiceController` | 계약의 공개 API 두 개를 제공한다. 녹음 파일과 확인·수정된 문장을 각각 받는다. |
| `VoiceService` | 현재 선택 검증, 후보 조회, Python 호출, 결과 대조, 실패 횟수 계산, 선택 미리보기 반환을 조정한다. |
| `VoiceProcessingClient` | 설정된 FastAPI 주소로 multipart/JSON 요청을 보내고 응답을 파싱한다. 연결·읽기 시간 제한을 설정한다. |
| 요청·응답 DTO | `CONTRACT.md`의 `text`, `context`, `candidates`, `fields`, `selection`, `failureCount`, `nextAction`을 타입으로 표현한다. |
| 상품·일정·옵션 조회 | Repository 또는 기존 Service를 통해 최신 DB 값을 읽는다. 일정 신청 가능 여부와 옵션 허용 규칙은 다른 신청 흐름과 같은 정책을 사용한다. |
| 예외 처리·보안 | 인증된 고객 요청만 받고, Python 오류를 공개 API 오류 형식으로 변환한다. |

## 구현 순서

1. 팀의 최신 공통 코드와 음성 브랜치를 통합하고, 현재 상품·일정·옵션 조회 API와 엔티티를 확인한다.
2. `VoiceProcessingClient`의 기본 주소를 설정값으로 받는다. 로컬 기본 예시는 `http://127.0.0.1:8001`이다. 배포 주소를 코드에 고정하지 않는다.
3. `POST /api/v1/voice/transcriptions`를 만든다. `audio` 파일의 존재, 형식, 크기·길이를 확인하고 Python의 `/internal/v1/transcribe`로 전달한다. 전사 문장이 비어 있으면 재녹음을 안내한다.
4. `POST /api/v1/voice/interpretations`를 만든다. 공백 문장과 잘못된 `context`를 거절하고, 현재 선택 ID가 실제로 존재하며 서로 맞는지 확인한다.
5. DB에서 해석에 쓸 **현재 선택 가능한 후보**를 조회한다. 상품·일정·옵션·등급 목록을 계약의 `candidates` 형식으로 Python에 전달한다. 회원 정보, JWT, 가격, 신청 인원 같은 불필요한 정보는 보내지 않는다.
6. Python의 필드별 `NOT_MENTIONED`·`RESOLVED`·`UNRESOLVED` 응답을 검증하고, `RESOLVED` 값은 DB 후보와 대조하여 `MATCHED`·`INVALID`·`AMBIGUOUS`로 결정한다.
7. 새 선택과 기존 선택을 합친 뒤 다시 업무 규칙을 검증한다. `failureCount`와 `nextAction`을 Spring에서 계산하여 미리보기를 반환한다.

## 후보 조회와 업무 규칙

- `tour`는 `TravelProduct`의 이름·테마와 대조한다. 비슷한 상품이 여러 개면 임의로 고르지 않고 `AMBIGUOUS`를 반환한다.
- `schedule`은 실제 등록된 `TravelSchedule`과 대조한다. 신청 가능 상태는 `RECRUITING_SCHEDULE` 또는 `CONFIRMED_SCHEDULE`이며, 출발일 7일 전 신청 기한, 정원, 상품 연결 관계를 확인한다. `CLOSED_SCHEDULE`·진행·완료·취소 일정은 후보에서 제외한다. 연도가 없는 날짜가 여러 일정에 해당하면 고객 확인이 필요하다.
- `grade`는 `CLASSIC`, `GRAND`, `PREMIUM` 중 하나로 다룬다. 허니문·효도 상품은 클래식 등급과 클래식 옵션을 허용하지 않는다. 상품별 가격의 `null` 등 실제 판매 가능 여부도 검증한다.
- 호텔·교통·식사·추가 옵션은 **해당 상품에 연결된** `TravelOption` 중 종류와 허용 등급이 맞는 것만 선택한다. 옵션명이 비슷해도 다른 상품의 옵션 ID를 사용하지 않는다. 추가 옵션은 여러 개를 선택할 수 있다.
- 기존 선택이 있어도 상품이나 등급이 바뀌면 이전 일정·옵션의 유효성을 다시 확인한다. 새 상품과 맞지 않는 이전 값은 그대로 확정하지 않고 확인 필요로 표시한다.
- 현재 선택의 ID와 Python의 해석 문구는 모두 검증 대상이다. Python은 선택 ID나 가격을 확정하지 않으며, Spring도 음성 미리보기 단계에서 가격을 최종 확정하지 않는다.
- 일정·등급·옵션 선택의 세부 규칙은 `TravelApplicationService`와 공유한다. 음성 전용으로 다른 신청 규칙을 만들지 않는다.

## 상태 합치기와 실패 판단

- Python이 `NOT_MENTIONED`로 반환한 항목은 현재 선택을 유지하되, 현재 DB 기준으로 다시 검증한다. 실패 횟수에는 포함하지 않는다.
- `UNRESOLVED`이거나 `RESOLVED`였지만 DB에서 `INVALID`·`AMBIGUOUS`가 된 항목은 기존 값을 조용히 덮어쓰지 않는다. 원래 발화와 확인 필요 상태를 화면에 반환한다.
- `failureCount`는 **투어·일정·호텔·식사** 네 항목에서 고객이 언급했으나 `UNRESOLVED`, `INVALID`, `AMBIGUOUS`인 항목의 수다. Python이 값을 반환하지 않아도 Spring이 직접 계산한다.
- `failureCount >= 2`이면 `RETRY_OR_GUI`, 그 외에는 `REVIEW`다. 등급·교통·추가 옵션의 오류도 화면에서 수정할 수 있어야 하지만 이 네 항목 실패 횟수에는 더하지 않는다.
- 선택 미리보기는 여행 신청이 아니다. 이 API에서 `TravelApplication`·`TravelParticipant`를 저장하거나 문자 발송을 요청하지 않는다.

## 보안·오류·운영

- 공개 음성 API는 고객의 인증과 `MEMBER` 권한을 확인한다. FastAPI 내부 API는 브라우저에 직접 노출하지 않고 Spring에서만 호출한다. Spring의 회원 토큰이나 회원 프로필 정보를 Python 요청의 별도 필드로 전달하지 않는다.
- 파일 업로드의 허용 형식(`audio/webm`, `audio/mp4`, `audio/wav`), 10 MiB·30초 제한은 공개 API와 Python 입력 단계에서 일관되게 적용한다. Content-Type만 믿지 않고 실제 디코딩 가능 여부도 확인한다.
- 빈 문장·누락 파일은 400, 초과 파일은 413, 지원하지 않는 파일은 415, 음성 인식 불가는 422, Python 오류는 502, 시간 초과는 504로 반환한다. 공개 오류 본문은 프로젝트의 `ResultDto` 형식(`success`, `code`, `message`)을 따른다.
- 연결 실패·잘못된 Python 응답을 성공으로 변환하지 않는다. 첫 STT 및 해석 요청에서는 모델 다운로드가 일어날 수 있으므로 실행 환경에 모델을 미리 준비하거나 호출 시간 제한을 고려한다. 내부 오류 상세나 스택 트레이스를 고객에게 보여주지 않는다. 화면은 재시도 또는 일반 GUI 선택으로 계속 진행할 수 있어야 한다.

## 연동 확인 항목

- 전사 문장을 고객이 수정한 뒤 해석에 사용하고, 미리보기까지만 생성되는가?
- 고객이 언급하지 않은 항목은 기존 선택을 유지하고 실패 횟수에 포함되지 않는가?
- 존재하지 않는 상품, 마감된 일정, 다른 상품의 옵션, 허용되지 않는 클래식 등급을 차단하는가?
- 같은 발화와 맞는 일정이 둘 이상이면 하나를 임의로 선택하지 않는가?
- 투어·일정·호텔·식사 중 두 항목 이상 실패하면 `RETRY_OR_GUI`가 나오는가?
- Python 중단·시간 초과·모델 오류 응답 때 기존 오류 형식과 GUI 대체 경로가 동작하는가?
- 음성 API 호출만으로 신청·가격 확정·문자 발송이 발생하지 않는가?

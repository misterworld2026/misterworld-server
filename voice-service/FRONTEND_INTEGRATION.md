# 프론트엔드 음성 선택 연동

React 화면은 Spring의 공개 API만 호출한다. Python `8001` 포트를 브라우저에서 직접 호출하지 않는다. 자세한 JSON은 [CONTRACT.md](CONTRACT.md)를 따른다.

## 화면 흐름

1. 고객이 녹음하면 브라우저가 `MediaRecorder` 결과를 `FormData`의 `audio` 필드로 `POST /api/v1/voice/transcriptions`에 보낸다. `Authorization: Bearer <access token>`을 포함한다. 허용 형식은 `audio/webm`, `audio/mp4`, `audio/wav`이고, 최대 30초·10 MiB다.
2. 응답의 `text`를 **수정 가능한 입력칸**에 표시한다. 고객은 문장을 고치거나 다시 녹음할 수 있다.
3. 고객이 해석을 요청하면 수정된 `text`와 현재 화면의 `context`를 `POST /api/v1/voice/interpretations`로 보낸다. 아직 선택하지 않은 단일 ID는 `null`, `optionIds.EXTRA`는 `[]`로 보낸다.
4. 응답의 `selection`과 `fields`를 함께 표시한다. `MATCHED`는 선택된 항목, `UNRESOLVED`·`INVALID`·`AMBIGUOUS`는 고객 확인이 필요한 항목이다. `NOT_MENTIONED`는 기존 화면 선택을 유지한다.
5. `nextAction=RETRY_OR_GUI`면 재녹음과 일반 화면 조작을 모두 제공한다. `REVIEW`여도 고객이 미해결 항목을 수정하고 최종 선택을 확인할 수 있어야 한다.
6. 고객이 확인한 값만 기존 여행 신청 화면으로 넘긴다. 음성 API 응답만으로 신청을 생성하거나 가격을 확정하지 않는다.

## 오류 처리

- 400: 빈 문장·누락 파일·유효하지 않은 현재 선택을 수정하도록 안내한다.
- 413: 녹음을 줄여 다시 시도한다.
- 415: 지원 형식으로 다시 녹음한다.
- 422: 음성을 인식하지 못했으므로 다시 녹음하거나 화면에서 선택한다.
- 502·504: 음성 서비스 문제이므로 재시도 또는 일반 화면 선택을 제공한다.
- 401·403: 로그인 상태와 회원 권한을 확인한다.

음성 미리보기에서 상품·일정·등급·옵션을 수정할 때는 최신 DB 선택지를 다시 조회해야 한다. 현재 브랜치의 일반 여행 신청 화면과 API 구현은 별도 담당 범위이므로, 프론트엔드가 합쳐질 때 이 문서의 흐름을 실제 신청 화면에 연결한다.

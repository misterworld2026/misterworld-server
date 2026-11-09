# 음성 선택 연동 계약 v1

이 문서는 React, Spring Boot, Python/FastAPI 사이의 **구현 계약**이다. 프로젝트 계획서의 음성 기반 상품 선택 목표와 설계 산출물 1.1의 3.2~3.4, 3.7, 4.3절을 바탕으로 한다. 아래 경로·필드명·상태값·업로드 한도는 해당 문서에 없던 부분을 연동을 위해 정한 것이다.

## 1. 호출 흐름과 책임

1. React는 녹음 파일을 Spring의 `POST /api/v1/voice/transcriptions`로 전송한다.
2. Spring은 파일을 내부 FastAPI의 `POST /internal/v1/transcribe`에 전달하고, 인식 문장을 React에 돌려준다.
3. 고객은 인식 문장을 확인·수정하거나 다시 녹음할 수 있다.
4. React는 확인한 문장과 현재 선택 정보를 Spring의 `POST /api/v1/voice/interpretations`로 전송한다. 설계 산출물의 `interpretVoice(text, context)`에 해당한다.
5. Spring은 DB에서 선택 가능한 상품·일정·옵션을 조회하여 내부 FastAPI의 `POST /internal/v1/interpret`에 문장·현재 선택·후보 목록을 보낸다. Python은 문장의 조건만 해석한다.
6. Spring은 Python 결과를 DB와 업무 규칙으로 검증하고 선택 미리보기를 React에 반환한다. 고객의 최종 확인 후에만 별도의 기존 여행 신청 흐름으로 이동한다.

브라우저는 FastAPI를 직접 호출하지 않는다. FastAPI에 회원 토큰이나 개인정보를 전달하지 않는다. 상품·일정·옵션 ID와 가격의 최종 판단은 Spring이 한다.

## 2. 공통 표현

| 항목 | 값 | 의미 |
| --- | --- | --- |
| Python 필드 상태 | `NOT_MENTIONED` | 고객이 해당 항목을 말하지 않음 |
| Python 필드 상태 | `RESOLVED` | 해당 항목을 말했고 값 또는 조건을 해석함 |
| Python 필드 상태 | `UNRESOLVED` | 해당 항목을 말했으나 값을 해석할 수 없음 |
| Spring 검증 상태 | `MATCHED` | 해석값을 등록 데이터의 유효한 선택지 하나에 대응시킴 |
| Spring 검증 상태 | `INVALID` | 해석값에 대응하는 허용 선택지가 없음 |
| Spring 검증 상태 | `AMBIGUOUS` | 허용 선택지가 둘 이상이어서 고객 확인이 필요함 |
| 다음 동작 | `REVIEW` | 결과를 고객에게 보여주고 확인·수정받음 |
| 다음 동작 | `RETRY_OR_GUI` | 음성 재입력 또는 화면 조작을 안내함 |

Python의 `value`는 고객이 말한 표현 또는 정규화한 조건 문자열이다. `NOT_MENTIONED`·`UNRESOLVED`이면 `value`는 `null`이다. `RESOLVED`여도 Spring 검증 전에는 실제 상품·일정·옵션으로 확정되지 않는다. 날짜에 연도가 없으면 Python이 임의로 연도를 채우지 않는다.

`tour`는 여행상품·테마를 뜻한다. `schedule`은 여행 출발 일정이다. `grade`의 계약 값은 `CLASSIC`, `GRAND`, `PREMIUM`이며, 실제 허용 여부는 상품별로 검증한다. `hotel`, `transport`, `meal`, `extras`는 여행옵션 종류 `HOTEL`, `TRANSPORT`, `MEAL`, `EXTRA`에 대응한다. 여러 추가 옵션을 말할 수 있으므로 `extras`는 배열이다.

고객이 말하지 않은 필드는 현재 선택값을 유지한다. 해석에 실패하거나 DB 검증에 실패한 필드는 현재 선택값을 임의로 덮어쓰지 않고 확인 필요 상태로 반환한다.

## 3. 공개 API: 음성 전사

`POST /api/v1/voice/transcriptions` — 인증된 고객의 `multipart/form-data` 요청. 파일 필드명은 `audio`다. 초기 계약의 허용 형식은 `audio/webm`, `audio/mp4`, `audio/wav`; 최대 30초·10 MiB다. 코덱이 실제로 디코딩되는지도 서버에서 확인한다.

성공 시 HTTP 200:

```json
{
  "text": "푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘"
}
```

인식한 문장이 비어 있으면 성공으로 취급하지 않고 재녹음을 안내한다. React는 이 문장을 고객에게 보여주며 수정할 수 있게 한다.

## 4. 공개 API: 문장 해석 및 선택 미리보기

`POST /api/v1/voice/interpretations` — 인증된 고객의 `application/json` 요청. `text`는 공백이 아닌 문자열이고, `context`는 현재 화면의 선택이다. 선택하지 않은 단일 항목은 `null`, 추가 옵션은 빈 배열을 사용한다. ID는 Spring이 재검증한다.

```json
{
  "text": "푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘",
  "context": {
    "productId": null,
    "scheduleId": null,
    "grade": null,
    "optionIds": {
      "HOTEL": null,
      "TRANSPORT": null,
      "MEAL": null,
      "EXTRA": []
    }
  }
}
```

성공 시 HTTP 200. 다음은 예시이며 ID는 실제 DB 조회 결과로 채운다.

```json
{
  "text": "푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘",
  "selection": {
    "productId": 1,
    "scheduleId": 10,
    "grade": "GRAND",
    "optionIds": {
      "HOTEL": 20,
      "TRANSPORT": null,
      "MEAL": null,
      "EXTRA": []
    }
  },
  "fields": {
    "tour": {"status": "MATCHED", "value": "푸껫 허니문"},
    "schedule": {"status": "MATCHED", "value": "3월 1일"},
    "grade": {"status": "MATCHED", "value": "GRAND"},
    "hotel": {"status": "MATCHED", "value": "오션뷰 객실"},
    "transport": {"status": "NOT_MENTIONED", "value": null},
    "meal": {"status": "NOT_MENTIONED", "value": null},
    "extras": []
  },
  "failureCount": 0,
  "nextAction": "REVIEW"
}
```

`fields.*.status`는 `NOT_MENTIONED`, `MATCHED`, `UNRESOLVED`, `INVALID`, `AMBIGUOUS` 중 하나다. `fields.*.value`는 음성에서 얻은 표현이며 실제 선택 ID가 아니다. `extras`의 각 원소도 `status`·`value` 형태를 사용한다. 미언급 항목은 기존 `context` 값이 `selection`에 유지될 수 있다.

`failureCount`는 **투어·일정·호텔·식사** 중 고객이 언급했으나 `UNRESOLVED`, `INVALID`, `AMBIGUOUS`인 항목 수다. `NOT_MENTIONED`는 세지 않는다. `failureCount >= 2`이면 `nextAction = RETRY_OR_GUI`; 그 외에는 `REVIEW`다. 등급·교통·추가 옵션의 오류도 `fields`에 드러내고 고객에게 수정을 요청하되, 설계 산출물의 네 항목 실패 횟수에는 더하지 않는다.

이 API는 미리보기만 반환한다. 신청 생성, 가격 계산·확정, 결제, DB 변경을 수행하지 않는다.

## 5. Spring ↔ Python 내부 API

- `POST /internal/v1/transcribe`: 공개 전사 API에서 받은 `audio` 파일을 전달하고 `{"text": "..."}`를 반환한다.
- `POST /internal/v1/interpret`: `text`, `context`, `candidates`를 JSON으로 받는다. `candidates`에는 Spring이 DB에서 조회한 상품(`id`, `name`, `theme`), 신청 가능한 일정(`id`, `productId`, `departureDate`), 옵션(`id`, `productId`, `type`, `name`, `grade`) 및 허용 등급을 넣는다. Python은 ID나 가격을 반환하지 않고 아래 필드 해석 결과를 반환한다.

내부 해석 요청 예시:

```json
{
  "text": "푸껫 허니문 3월 1일 출발로 그랜드 등급에 오션뷰 객실로 해줘",
  "context": {
    "productId": null,
    "scheduleId": null,
    "grade": null,
    "optionIds": {"HOTEL": null, "TRANSPORT": null, "MEAL": null, "EXTRA": []}
  },
  "candidates": {
    "products": [
      {"id": 1, "name": "푸껫 허니문 5박 7일", "theme": "HONEYMOON", "availableGrades": ["GRAND", "PREMIUM"]}
    ],
    "schedules": [
      {"id": 10, "productId": 1, "departureDate": "2027-03-01"}
    ],
    "options": [
      {"id": 20, "productId": 1, "type": "HOTEL", "name": "오션뷰 객실 업그레이드", "grade": null}
    ]
  }
}
```

후보의 ID는 Python이 문맥을 이해하는 데만 사용한다. 아래 응답에는 ID를 넣지 않는다. 후보 일정은 Spring이 조회 시점에 신청 가능한 일정으로 제한한다.

내부 해석 응답 예시:

```json
{
  "fields": {
    "tour": {"state": "RESOLVED", "value": "푸껫 허니문"},
    "schedule": {"state": "RESOLVED", "value": "3월 1일"},
    "grade": {"state": "RESOLVED", "value": "GRAND"},
    "hotel": {"state": "RESOLVED", "value": "오션뷰 객실"},
    "transport": {"state": "NOT_MENTIONED", "value": null},
    "meal": {"state": "NOT_MENTIONED", "value": null},
    "extras": []
  }
}
```

Spring은 Python 응답의 필드와 상태값을 검증하고, 실제 데이터와 대조한다. 동일 표현에 맞는 일정·옵션이 여러 개면 하나를 임의로 선택하지 않고 `AMBIGUOUS`로 반환한다. 프롬프트에는 선택 가능한 항목과 예시를 제공하되, 프롬프트 결과만으로 유효성을 판단하지 않는다.

## 6. 오류 계약

공개 API 오류는 프로젝트의 기존 오류 형식 `{"success": false, "code": 400, "message": "..."}`을 따른다.

| 상황 | HTTP 상태 | 화면 처리 |
| --- | --- | --- |
| 빈 문장·누락된 음성 파일 | 400 | 입력 또는 재녹음 요청 |
| 음성 파일이 10 MiB 또는 30초 초과 | 413 | 짧게 다시 녹음 요청 |
| 지원하지 않거나 디코딩할 수 없는 음성 파일 | 415 | 지원 형식 안내 |
| 발화를 인식하지 못함 | 422 | 재녹음 또는 GUI 선택 안내 |
| Python 서비스 오류 | 502 | GUI 선택 가능하게 안내 |
| Python 서비스 응답 시간 초과 | 504 | 재시도 또는 GUI 선택 안내 |

내부 FastAPI 오류 응답은 `{"error": {"code": "TRANSCRIPTION_FAILED", "message": "..."}}` 형태로 반환한다. Spring은 내부 오류 내용을 그대로 고객에게 노출하지 않고 위 공개 오류 형식으로 변환한다. 해석 결과의 개별 항목 실패는 HTTP 오류가 아니라 4절의 `fields`와 `nextAction`으로 반환한다.

## 7. 계약 검증 기준

- 전사한 문장을 고객이 확인·수정한 뒤 해석 요청에 사용할 수 있다.
- 미언급 항목은 이전 선택을 유지하며 실패 횟수에 포함되지 않는다.
- 존재하지 않거나 허용되지 않은 상품·일정·옵션은 Spring 검증에서 차단된다.
- 네 대상 항목 중 두 개 이상 실패하면 `RETRY_OR_GUI`가 반환된다.
- 정상 결과도 고객 확인 전에는 여행 신청을 생성하지 않는다.
- 계획서의 **음성 명령 성공률 80%**는 별도 발화 목록과 판정 기준을 정한 뒤 통합 단계에서 측정한다. 이 문서의 HTTP 성공률과 혼동하지 않는다.

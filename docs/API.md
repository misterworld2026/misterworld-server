# API 계약·화면 및 계층 대응표

근거: 사용자 프롬프트의 기존 API 목록. 실제 API 이미지와 HTML은 미제공/미발견이다. 기존 경로·HTTP 메서드를 유지했다. 성공은 DTO JSON이며 Entity를 직접 노출하지 않는다. 목록은 page=0, size=20 기본, size 최대 100. 현재 정렬은 서버 고정이며 사용자 지정 정렬·상태 필터는 미구현이다. Page JSON 계약은 Spring Data 기본 직렬화이며 프론트 협의 후 고정 응답 구조로 바꿀 필요가 있다.

모든 `/api/admin/**`는 STAFF. 예외는 `/api/admin/auth/login`만 공개다. `/api/my/**`, 상품·일정 조회, 신청, 음성은 인증이 필요하다. `/api/auth/signup`, `/api/auth/login`, `/api/auth/refresh`는 공개다. refresh는 유효 REFRESH JWT가 필수다. STAFF 공개 가입은 허용하지 않는다.

| 화면·기능 | 기존 API | Controller 메서드 | Service 메서드 | Entity | DB 테이블·컬럼 | 테스트 |
|---|---|---|---|---|---|---|
| 직원 로그인 | POST /api/admin/auth/login | AdminAuthController.login | AuthService.login | Member | 회원.이메일/비밀번호/회원구분 | ApiIntegrationTests |
| 직원 가입 | POST /api/admin/auth/signup | AdminAuthController.signup | AuthService.createStaffAccount | Member | 회원 | 정책 대기: 인증 후 409 |
| 토큰 갱신·로그아웃 | POST /api/auth/refresh, POST /api/auth/logout | AuthController.refresh/logout | AuthService.refresh/logout | Member + TokenSession(추가) | 회원 + 인증세션(추가) | ApiIntegrationTests |
| 상품 목록 | GET /api/tours | TravelProductController.getProducts | TravelProductService.getProducts | TravelProduct | 여행상품.테마/상품명/등급별 가격 | ProductSelectionTests, HTTP CRUD 일부 |
| 상품 상세 | GET /api/tours/{tourId} | TravelProductController.getProduct | TravelProductService.getProduct | TravelProduct/TravelOption | 여행상품/여행옵션 | 조회 응답 구성, 별도 HTTP 테스트 미실행 |
| 옵션 선택 | GET /api/tours/{tourId}/options | TravelProductController.getOptions | TravelProductService.getOptions/validateSelection | TravelOption | 여행옵션.옵션종류/옵션등급/기본등급 | ProductSelectionTests |
| 일정 목록 | GET /api/tours/schedules, GET /api/tours/{tourId}/schedules | TravelScheduleController.getSchedules | TravelScheduleService.getSchedules/buildScheduleSelectionResponse | TravelSchedule | 여행일정.출발일/종료일/여행상태/현재신청인원 | API 기동, 선택 가능 여부는 정책 대기로 false |
| 본인 정보 | GET/PUT /api/my/profile | MemberController.getMyInfo/updateMyInfo | MemberService.getMyInfo/updateMyInfo | Member | 회원.이름/주소/연락처 | ApiIntegrationTests(조회·권한) |
| 본인 신청 | GET /api/my/applications, GET /api/my/applications/{applicationId} | TravelApplicationController.getMyApplication/getApplication | TravelApplicationService.getMyApplication/getApplication | TravelApplication | 여행신청.회원_ID/구성/가격/신청상태 | ApiIntegrationTests(없는 대상 404), 실제 데이터 분리 추가 검증 필요 |
| 여행 이력 | GET /api/my/trips/history | TravelApplicationController.getTravelHistory | TravelApplicationService.getTravelHistory | TravelApplication/TravelSchedule | COMPLETED_SCHEDULE + CONFIRMED 조건 | 조회 JPQL 기동 검증, 할인·횟수 미구현 |
| 고객 취소 | PUT /api/my/applications/{applicationId}/cancel | TravelApplicationController.cancelApplication | TravelApplicationService.cancelApplication | TravelApplication | 여행신청 | 본인 조회 후 정책 대기 409, 동작 미구현 |
| 신청 | POST /api/reservations | TravelApplicationController.apply | TravelApplicationService.apply | TravelApplication/TravelParticipant | 여행신청/여행객 | ApiIntegrationTests(정책 미결정 차단 409), 신청 구현 아님 |
| 본인 문자 | GET /api/my/notifications | NotificationController.getMyNotifications(추가 클래스) | SmsNotificationService.getNotifications(추가) | SmsNotification | 문자알림.여행신청_ID/발송상태 | 리포지토리 기동, 실제 발송 미구현 |
| 상품 관리 | POST /api/admin/tours, PUT /api/admin/tours/{tourId} | TravelProductController.createProduct/updateProduct/saveProduct | TravelProductService.saveProduct/saveOption | TravelProduct/TravelOption | 여행상품/여행옵션 | ApiIntegrationTests, ProductSelectionTests |
| 일정 등록 | POST /api/admin/tours/{tourId}/schedules | TravelScheduleController.createSchedule | TravelScheduleService.saveSchedule | TravelSchedule | 여행일정 | ApiIntegrationTests |
| 일정 날짜 수정 | PATCH /api/admin/schedules/{scheduleId} | TravelScheduleController.updateSchedule | TravelScheduleService.updateSchedule/validateScheduleChange | TravelSchedule | 여행일정.출발일/종료일 | 잠금 구현, MySQL 경합 미검증 |
| 일정 삭제 | DELETE /api/admin/schedules/{scheduleId} | TravelScheduleController.deleteSchedule | TravelScheduleService.deleteSchedule | TravelSchedule | 여행일정 (신청 이력 존재 시 거절) | ApiIntegrationTests(신청 없는 일정) |
| 직원 신청 조회 | GET /api/admin/reservations | TravelApplicationController.getApplications(추가) | TravelApplicationService.getApplications(추가) | TravelApplication | 여행신청 | 기동 검증, 상세 필터 미구현 |
| 재고 등록·조회·수정 | POST/GET /api/admin/inventory, PATCH /api/admin/inventory/{id} | ItemInventoryController.registerInventory/getInventoryList/updateInventory | ItemInventoryService.registerInventory/getInventoryList/updateInventory | ItemInventory | 물품재고.재고수량/재고상태/수정일시 | ApiIntegrationTests: -1/0/1/3/4/5/1.5, NameContractTests |
| 직원 문자 조회 | GET /api/admin/notifications | NotificationController.getNotifications(추가) | SmsNotificationService.getNotifications(추가) | SmsNotification | 문자알림 | 권한 규칙, 발송 테스트 없음 |

외부 URI `tourId`는 `@PathVariable("tourId") Long productId`로 매핑한다. `id` 재고 경로는 `inventoryId`로 매핑한다. 회원 ID 입력은 받지 않는다.

## 추가 API (기존 확정 API로 주장하지 않음)

| API | 이유 | 현재 상태 |
|---|---|---|
| POST /api/auth/signup | 고객 가입이 PDF에 있고 기존 코드·이미지 경로 없음 | 구현 |
| POST /api/auth/login | 고객 로그인 필요 | 구현 |
| POST /api/voice/interpret | 음성 텍스트 해석 확인 | 추가 설계, 미연결 시 503 |
| 견적 API | 재확인 가능한 서버 견적 필요 | 경로·계약 미확정, 미구현 |
| 일정 취소 API | 삭제와 다른 기능 | 경로 미확정, 미구현 |
| 일정별 예약 현황 API | PDF getReservationStatus | 반환 DTO 충돌 질문 대기, 미구현 |
| 대시보드·결제 | 실제 화면·범위 확인 필요 | 미구현 |

## 요청·응답 예시

가입: POST /api/auth/signup

```json
{"email":"member@example.com","password":"example-password","passwordConfirmation":"example-password","name":"홍길동","address":"서울","phoneNumber":"01012345678"}
```

201 응답 예시 (`id`는 서버 생성):

```json
{"id":1,"email":"member@example.com","role":"MEMBER"}
```

로그인: POST /api/auth/login

```json
{"email":"member@example.com","password":"example-password"}
```

```json
{"accessToken":"<ACCESS JWT>","refreshToken":"<REFRESH JWT>","role":"MEMBER"}
```

이후 `Authorization: Bearer <ACCESS JWT>` 사용. refresh에는 `{"refreshToken":"<REFRESH JWT>"}`를 보낸다. 비밀번호 10~72자이며 BCrypt 한계 때문에 UTF-8 72바이트도 검사한다. 추가 보안 설계다.

재고 등록 (STAFF):

```json
{"productId":1,"itemName":"골프공","quantity":3}
```

응답 예시:

```json
{"id":1,"productId":1,"itemName":"골프공","quantity":3,"status":"LOW_STOCK","updatedAt":"2026-11-03T05:00:00"}
```

수정 `PATCH /api/admin/inventory/1`: `{"quantity":0}`. 0은 OUT_OF_STOCK, 1~3은 LOW_STOCK, 4 이상 NORMAL. 수정일시는 주입 Clock 기준 UTC LocalDateTime이다. 재고 자동 차감이나 재고 부족 신청 차단은 없다.

상품 등록 (STAFF):

```json
{"name":"골프 상품","theme":"골프","description":"설명","classicPrice":100000,"grandPrice":200000,"premiumPrice":300000,"options":[{"type":"HOTEL","name":"3등급 호텔","grade":"CLASSIC","additionalPrice":0,"defaultForGrade":"CLASSIC"},{"type":"TRANSPORT","name":"10인 고급차량","grade":"CLASSIC","additionalPrice":0,"defaultForGrade":"CLASSIC"},{"type":"MEAL","name":"도시락","grade":"CLASSIC","additionalPrice":0,"defaultForGrade":"CLASSIC"}]}
```

허니문·효도 classicPrice는 null. 옵션은 상품 PUT에 함께 포함한다. 수정 시 기존 모든 옵션 id를 포함해야 하며, 옵션 삭제·등급별 기본 구성 완전성 검증은 아직 추가 작업이 필요하다. 상품 정의의 금액을 저장할 수 있지만 과금·견적 금액 계산은 미구현이다.

일정 등록: `{"departureDate":"2027-01-01","endDate":"2027-01-02"}`. Java Entity 원문 필드는 `arrivalDate`이며 DB `종료일`에 매핑, validateDates 매개변수는 본문 `endDate` 유지.

## 오류 계약

```json
{"success":false,"message":"정책 결정이 필요합니다. docs/REVIEW.md를 확인하세요.","status":409,"code":"POLICY_UNRESOLVED"}
```

실제 HTTP 상태도 409다. 일반 입력 400, 토큰·로그인 401, 권한 403, 대상 부재·타인 대상 404, 중복·제약 409, 미연결 음성 503. 검증 오류는 필드 값·비밀번호를 반환하지 않는다. 토큰과 개인정보는 로그에 남기지 않는다.


재고 기준은 2026-11-03 사용자 결정으로 PDF의 4개 이하에서 **3개 이하**로 변경했다. V4는 기존 물품 수량을 유지하며 저장된 상태만 재분류한다.

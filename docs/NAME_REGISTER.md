# 원문 명칭 대장 (검토 진행 중)

이 대장은 확인한 선언만 수록한다. 전체 PDF의 모든 메시지·조건·액션을 수록한 완성본이 아니다. 타입 미명시 부분은 추가 설계로 구분한다. 구현 위치가 있어도 동작 완료를 뜻하지 않는다.

| 종류 | 원문 이름 | 소속 클래스·문맥 | 문서·페이지 | 명시된 타입·매개변수 | 구현 위치 | 충돌·비고 |
|---|---|---|---|---|---|---|
| Entity | Member | Member | 설계 p.6·12 | 클래스 | domain/Member.java | `회원` 매핑 |
| 필드 | id | Member | 설계 p.6 | Long | domain/Member.java#id | DB `회원_ID`;  |
| 필드 | email | Member | 설계 p.6 | String | domain/Member.java#email | DB `이메일`;  |
| 필드 | password | Member | 설계 p.6 | String | domain/Member.java#password | DB `비밀번호`;  |
| 필드 | name | Member | 설계 p.6 | String | domain/Member.java#name | DB `이름`;  |
| 필드 | address | Member | 설계 p.6 | String | domain/Member.java#address | DB `주소`;  |
| 필드 | phoneNumber | Member | 설계 p.6 | String | domain/Member.java#phoneNumber | DB `연락처`;  |
| 필드 | travelCount | Member | 설계 p.6 | Integer | domain/Member.java#travelCount | DB `Field`;  |
| 필드 | role | Member | 설계 p.6 | Role | domain/Member.java#role | DB `회원구분`;  |
| Entity | TravelProduct | TravelProduct | 설계 p.6·12 | 클래스 | domain/TravelProduct.java | `여행상품` 매핑 |
| 필드 | id | TravelProduct | 설계 p.6 | Long | domain/TravelProduct.java#id | DB `여행상품_ID`;  |
| 필드 | name | TravelProduct | 설계 p.6 | String | domain/TravelProduct.java#name | DB `상품명`;  |
| 필드 | theme | TravelProduct | 설계 p.6 | String | domain/TravelProduct.java#theme | DB `테마`;  |
| 필드 | description | TravelProduct | 설계 p.6 | String | domain/TravelProduct.java#description | DB `상품설명`;  |
| 필드 | classicPrice | TravelProduct | 설계 p.6 | Integer | domain/TravelProduct.java#classicPrice | DB `클래식_가격`;  |
| 필드 | grandPrice | TravelProduct | 설계 p.6 | Integer | domain/TravelProduct.java#grandPrice | DB `그랜드_가격`;  |
| 필드 | premiumPrice | TravelProduct | 설계 p.6 | Integer | domain/TravelProduct.java#premiumPrice | DB `프리미엄_가격`;  |
| Entity | TravelOption | TravelOption | 설계 p.6·12 | 클래스 | domain/TravelOption.java | `여행옵션` 매핑 |
| 필드 | id | TravelOption | 설계 p.6 | Long | domain/TravelOption.java#id | DB `여행옵션_ID`;  |
| 필드 | travelProduct | TravelOption | 설계 p.6 | TravelProduct | domain/TravelOption.java#travelProduct | DB `여행상품_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | type | TravelOption | 설계 p.6 | String | domain/TravelOption.java#type | DB `옵션종류`;  |
| 필드 | name | TravelOption | 설계 p.6 | String | domain/TravelOption.java#name | DB `옵션명`;  |
| 필드 | grade | TravelOption | 설계 p.6 | String | domain/TravelOption.java#grade | DB `옵션등급`;  |
| 필드 | additionalPrice | TravelOption | 설계 p.6 | Integer | domain/TravelOption.java#additionalPrice | DB `추가금액`;  |
| 필드 | description | TravelOption | 설계 p.6 | String | domain/TravelOption.java#description | DB `옵션설명`;  |
| 필드 | defaultForGrade | TravelOption | 설계 p.6 | String | domain/TravelOption.java#defaultForGrade | DB `기본등급`;  |
| Entity | TravelSchedule | TravelSchedule | 설계 p.6·12 | 클래스 | domain/TravelSchedule.java | `여행일정` 매핑 |
| 필드 | travelScheduleId | TravelSchedule | 설계 p.6 | Long | domain/TravelSchedule.java#travelScheduleId | DB `여행일정_ID`;  |
| 필드 | travelProduct | TravelSchedule | 설계 p.6 | TravelProduct | domain/TravelSchedule.java#travelProduct | DB `여행상품_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | departureDate | TravelSchedule | 설계 p.6 | LocalDate | domain/TravelSchedule.java#departureDate | DB `출발일`;  |
| 필드 | arrivalDate | TravelSchedule | 설계 p.6 | LocalDate | domain/TravelSchedule.java#arrivalDate | DB `종료일`;  |
| 필드 | status | TravelSchedule | 설계 p.6 | ScheduleStatus | domain/TravelSchedule.java#status | DB `여행상태`;  |
| 필드 | applicantCount | TravelSchedule | 설계 p.6 | Integer | domain/TravelSchedule.java#applicantCount | DB `현재신청인원`;  |
| Entity | TravelApplication | TravelApplication | 설계 p.6·12 | 클래스 | domain/TravelApplication.java | `여행신청` 매핑 |
| 필드 | id | TravelApplication | 설계 p.6 | Long | domain/TravelApplication.java#id | DB `여행신청_ID`;  |
| 필드 | member | TravelApplication | 설계 p.6 | Member | domain/TravelApplication.java#member | DB `회원_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | travelSchedule | TravelApplication | 설계 p.6 | TravelSchedule | domain/TravelApplication.java#travelSchedule | DB `여행일정_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | tourGrade | TravelApplication | 설계 p.6 | String | domain/TravelApplication.java#tourGrade | DB `투어등급`;  |
| 필드 | hotelOption | TravelApplication | 설계 p.6 | String | domain/TravelApplication.java#hotelOption | DB `호텔옵션`;  |
| 필드 | transportOption | TravelApplication | 설계 p.6 | String | domain/TravelApplication.java#transportOption | DB `교통옵션`;  |
| 필드 | mealOption | TravelApplication | 설계 p.6 | String | domain/TravelApplication.java#mealOption | DB `식사옵션`;  |
| 필드 | extraOption | TravelApplication | 설계 p.6 | String | domain/TravelApplication.java#extraOption | DB `추가옵션`;  |
| 필드 | applicantCount | TravelApplication | 설계 p.6 | Integer | domain/TravelApplication.java#applicantCount | DB `신청인원`;  |
| 필드 | basePrice | TravelApplication | 설계 p.6 | Integer | domain/TravelApplication.java#basePrice | DB `기본가격`;  |
| 필드 | additionalOptionPrice | TravelApplication | 설계 p.6 | Integer | domain/TravelApplication.java#additionalOptionPrice | DB `옵션추가금액`;  |
| 필드 | discountRate | TravelApplication | 설계 p.6 | Integer | domain/TravelApplication.java#discountRate | DB `할인율`;  |
| 필드 | discountAmount | TravelApplication | 설계 p.6 | Integer | domain/TravelApplication.java#discountAmount | DB `할인금액`;  |
| 필드 | totalPrice | TravelApplication | 설계 p.6 | Integer | domain/TravelApplication.java#totalPrice | DB `최종가격`;  |
| 필드 | status | TravelApplication | 설계 p.6 | ApplicationStatus | domain/TravelApplication.java#status | DB `신청상태`;  |
| 필드 | appliedAt | TravelApplication | 설계 p.6 | LocalDateTime | domain/TravelApplication.java#appliedAt | DB `신청일시`;  |
| Entity | TravelParticipant | TravelParticipant | 설계 p.6·12 | 클래스 | domain/TravelParticipant.java | `여행객` 매핑 |
| 필드 | id | TravelParticipant | 설계 p.6 | Long | domain/TravelParticipant.java#id | DB `여행객_ID`;  |
| 필드 | travelApplication | TravelParticipant | 설계 p.6 | TravelApplication | domain/TravelParticipant.java#travelApplication | DB `여행신청_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | name | TravelParticipant | 설계 p.6 | String | domain/TravelParticipant.java#name | DB `이름`;  |
| 필드 | phoneNumber | TravelParticipant | 설계 p.6 | String | domain/TravelParticipant.java#phoneNumber | DB `연락처`;  |
| 필드 | birthDate | TravelParticipant | 설계 p.6 | LocalDate | domain/TravelParticipant.java#birthDate | DB `생년월일`;  |
| 필드 | participantType | TravelParticipant | 설계 p.6 | ParticipantType | domain/TravelParticipant.java#participantType | DB `참가자구분`;  |
| Entity | ItemInventory | ItemInventory | 설계 p.6·12 | 클래스 | domain/ItemInventory.java | `물품재고` 매핑 |
| 필드 | id | ItemInventory | 설계 p.6 | Long | domain/ItemInventory.java#id | DB `물품재고_ID`;  |
| 필드 | travelProduct | ItemInventory | 설계 p.6 | TravelProduct | domain/ItemInventory.java#travelProduct | DB `여행상품_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | itemName | ItemInventory | 설계 p.6 | String | domain/ItemInventory.java#itemName | DB `물품명`;  |
| 필드 | quantity | ItemInventory | 설계 p.6 | Integer | domain/ItemInventory.java#quantity | DB `재고수량`;  |
| 필드 | status | ItemInventory | 설계 p.6 | InventoryStatus | domain/ItemInventory.java#status | DB `재고상태`;  |
| 필드 | updatedAt | ItemInventory | 설계 p.6 | LocalDateTime | domain/ItemInventory.java#updatedAt | DB `수정일시`;  |
| Entity | SmsNotification | SmsNotification | 설계 p.6·12 | 클래스 | domain/SmsNotification.java | `문자알림` 매핑 |
| 필드 | id | SmsNotification | 설계 p.6 | Long | domain/SmsNotification.java#id | DB `문자알림_ID`;  |
| 필드 | travelApplication | SmsNotification | 설계 p.6 | TravelApplication | domain/SmsNotification.java#travelApplication | DB `여행신청_ID`; 관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계 |
| 필드 | notificationType | SmsNotification | 설계 p.6·39 | NotificationType | domain/SmsNotification.java#notificationType | DB `알림종류`; p.39 명시, 타입명 NotificationType은 추가 설계 |
| 필드 | recipientNumber | SmsNotification | 설계 p.6 | String | domain/SmsNotification.java#recipientNumber | DB `수신번호`;  |
| 필드 | message | SmsNotification | 설계 p.6 | String | domain/SmsNotification.java#message | DB `메시지내용`;  |
| 필드 | status | SmsNotification | 설계 p.6 | NotificationStatus | domain/SmsNotification.java#status | DB `발송상태`;  |
| 필드 | sentAt | SmsNotification | 설계 p.6 | LocalDateTime | domain/SmsNotification.java#sentAt | DB `발송일시`;  |
| Enum | Role | domain | 설계 p.6·12 | MEMBER, STAFF | domain/Role.java | 상태 상수는 p.12·36~39 |
| Enum | ScheduleStatus | domain | 설계 p.6·12 | RECRUITING_SCHEDULE, CONFIRMED_SCHEDULE, CLOSED_SCHEDULE, IN_PROGRESS_SCHEDULE, COMPLETED_SCHEDULE, CANCELLED_SCHEDULE | domain/ScheduleStatus.java | 상태 상수는 p.12·36~39 |
| Enum | ApplicationStatus | domain | 설계 p.6·12 | PENDING, CONFIRMED, CANCELLED | domain/ApplicationStatus.java | 상태 상수는 p.12·36~39 |
| Enum | InventoryStatus | domain | 설계 p.6·12 | NORMAL, LOW_STOCK, OUT_OF_STOCK | domain/InventoryStatus.java | 상태 상수는 p.12·36~39 |
| Enum | NotificationStatus | domain | 설계 p.6·12 | PENDING, SENT, FAILED | domain/NotificationStatus.java | 상태 상수는 p.12·36~39 |
| Enum | NotificationType | domain | 설계 p.6·12 | DEPARTURE_CONFIRMED, SCHEDULE_CANCELLED | domain/NotificationType.java | 타입명·상수명은 추가 설계 |
| Enum | ParticipantType | domain | 설계 p.6·12 | APPLICANT, COMPANION | domain/ParticipantType.java | 타입명은 원문, 상수명은 추가 설계 |
| 구현 메서드 | signup | AuthController | 설계 p.6·8·14~17 | signup(request); 타입/반환형은 개별 원문 확인 필요 | controller/AuthController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | login | AuthController | 설계 p.6·8·14~17 | login(request); 타입/반환형은 개별 원문 확인 필요 | controller/AuthController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | logout | AuthController | 설계 p.6·8·14~17 | logout(); 타입/반환형은 개별 원문 확인 필요 | controller/AuthController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | signup | AdminAuthController | 설계 p.6·8 | signup(request); 타입/반환형은 개별 원문 확인 필요 | controller/AdminAuthController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | signup | AuthService | 설계 p.6·9·14~17 | signup(request); 타입/반환형은 개별 원문 확인 필요 | service/AuthService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | createStaffAccount | AuthService | 설계 p.6·9·14~17 | createStaffAccount(request); 타입/반환형은 개별 원문 확인 필요 | service/AuthService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | login | AuthService | 설계 p.6·9·14~17 | login(request); 타입/반환형은 개별 원문 확인 필요 | service/AuthService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | logout | AuthService | 설계 p.6·9·14~17 | logout(); 타입/반환형은 개별 원문 확인 필요 | service/AuthService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validatePasswordConfirmation | AuthService | 설계 p.6·9·14~17 | validatePasswordConfirmation(request); 타입/반환형은 개별 원문 확인 필요 | service/AuthService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getMyInfo | MemberController | 설계 p.6·8 | getMyInfo(memberId); 타입/반환형은 개별 원문 확인 필요 | controller/MemberController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateMyInfo | MemberController | 설계 p.6·8 | updateMyInfo(memberId, request); 타입/반환형은 개별 원문 확인 필요 | controller/MemberController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getMyInfo | MemberService | 설계 p.6·9 | getMyInfo(memberId); 타입/반환형은 개별 원문 확인 필요 | service/MemberService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateMyInfo | MemberService | 설계 p.6·9 | updateMyInfo(memberId, request); 타입/반환형은 개별 원문 확인 필요 | service/MemberService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getProducts | TravelProductController | 설계 p.6·8·18~19·29~30 | getProducts(); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getProducts | TravelProductController | 설계 p.6·8·18~19·29~30 | getProducts(theme); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getProduct | TravelProductController | 설계 p.6·8·18~19·29~30 | getProduct(productId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getOptions | TravelProductController | 설계 p.6·8·18~19·29~30 | getOptions(productId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | createProduct | TravelProductController | 설계 p.6·8·18~19·29~30 | createProduct(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateProduct | TravelProductController | 설계 p.6·8·18~19·29~30 | updateProduct(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | saveOption | TravelProductController | 설계 p.6·8·18~19·29~30 | saveOption(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | saveProduct | TravelProductController | 설계 p.6·8·18~19·29~30 | saveProduct(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelProductController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getProducts | TravelProductService | 설계 p.6·9·18~19·23·29~30 | getProducts(); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getProducts | TravelProductService | 설계 p.6·9·18~19·23·29~30 | getProducts(theme); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getProduct | TravelProductService | 설계 p.6·9·18~19·23·29~30 | getProduct(productId); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getOptions | TravelProductService | 설계 p.6·9·18~19·23·29~30 | getOptions(productId); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | saveProduct | TravelProductService | 설계 p.6·9·18~19·23·29~30 | saveProduct(request); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | saveOption | TravelProductService | 설계 p.6·9·18~19·23·29~30 | saveOption(request); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateSelection | TravelProductService | 설계 p.6·9·18~19·23·29~30 | validateSelection(productId, grade, optionIds); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | buildAllowedGradesAndDefaults | TravelProductService | 설계 p.6·9·18~19·23·29~30 | buildAllowedGradesAndDefaults(); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | prepareNewProduct | TravelProductService | 설계 p.6·9·18~19·23·29~30 | prepareNewProduct(); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateThemeGradesPricesAndOptions | TravelProductService | 설계 p.6·9·18~19·23·29~30 | validateThemeGradesPricesAndOptions(); 타입/반환형은 개별 원문 확인 필요 | service/TravelProductService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getSchedules | TravelScheduleController | 설계 p.6·8·19~20·31~33 | getSchedules(productId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelScheduleController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | createSchedule | TravelScheduleController | 설계 p.6·8·19~20·31~33 | createSchedule(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelScheduleController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateSchedule | TravelScheduleController | 설계 p.6·8·19~20·31~33 | updateSchedule(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelScheduleController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getReservationStatus | TravelScheduleController | 설계 p.6·8·19~20·31~33 | getReservationStatus(scheduleId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelScheduleController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | saveSchedule | TravelScheduleController | 설계 p.6·8·19~20·31~33 | saveSchedule(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelScheduleController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getSchedules | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | getSchedules(productId); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | saveSchedule | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | saveSchedule(request); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getReservationStatus | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | getReservationStatus(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateApplicantCount | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | updateApplicantCount(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | confirmDeparture | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | confirmDeparture(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | completeTravel | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | completeTravel(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateAndLockSchedule | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | validateAndLockSchedule(scheduleId, applicantCount); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | buildScheduleSelectionResponse | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | buildScheduleSelectionResponse(schedules); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateDates | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | validateDates(departureDate, endDate); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateScheduleChange | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | validateScheduleChange(schedule, request, hasApplications); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | checkDepartureCondition | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | checkDepartureCondition(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | calculateValidApplicantCount | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | calculateValidApplicantCount(applications); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | isDepartureConditionMet | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | isDepartureConditionMet(schedule, applications); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | calculateReservationStatus | TravelScheduleService | 설계 p.6·9·19~20·23~26·31~33·36~37 | calculateReservationStatus(schedule, applications); 타입/반환형은 개별 원문 확인 필요 | service/TravelScheduleService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | apply | TravelApplicationController | 설계 p.6·8·23~24·28 | apply(request); 타입/반환형은 개별 원문 확인 필요 | controller/TravelApplicationController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getMyApplication | TravelApplicationController | 설계 p.6·8·23~24·28 | getMyApplication(memberId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelApplicationController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getApplication | TravelApplicationController | 설계 p.6·8·23~24·28 | getApplication(applicationId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelApplicationController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getTravelHistory | TravelApplicationController | 설계 p.6·8·23~24·28 | getTravelHistory(memberId); 타입/반환형은 개별 원문 확인 필요 | controller/TravelApplicationController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | apply | TravelApplicationService | 설계 p.6·9·23~24·28 | apply(request); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | apply | TravelApplicationService | 설계 p.6·9·23~24·28 | apply(memberId, request); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getMyApplication | TravelApplicationService | 설계 p.6·9·23~24·28 | getMyApplication(memberId); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getApplication | TravelApplicationService | 설계 p.6·9·23~24·28 | getApplication(applicationId); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getTravelHistory | TravelApplicationService | 설계 p.6·9·23~24·28 | getTravelHistory(memberId); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | calculateFinalPrice | TravelApplicationService | 설계 p.6·9·23~24·28 | calculateFinalPrice(request); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | calculateDiscount | TravelApplicationService | 설계 p.6·9·23~24·28 | calculateDiscount(request); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | calculatePrice | TravelApplicationService | 설계 p.6·9·23~24·28 | calculatePrice(selection, applicantCount); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | applyLoyaltyDiscount | TravelApplicationService | 설계 p.6·9·23~24·28 | applyLoyaltyDiscount(completedTrips); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | buildTravelHistoryResponse | TravelApplicationService | 설계 p.6·9·23~24·28 | buildTravelHistoryResponse(applications); 타입/반환형은 개별 원문 확인 필요 | service/TravelApplicationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getInventory | ItemInventoryController | 설계 p.6·8·34~35 | getInventory(); 타입/반환형은 개별 원문 확인 필요 | controller/ItemInventoryController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | registerItem | ItemInventoryController | 설계 p.6·8·34~35 | registerItem(request); 타입/반환형은 개별 원문 확인 필요 | controller/ItemInventoryController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateQuantity | ItemInventoryController | 설계 p.6·8·34~35 | updateQuantity(itemId, quantity); 타입/반환형은 개별 원문 확인 필요 | controller/ItemInventoryController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | registerInventory | ItemInventoryController | 설계 p.6·8·34~35 | registerInventory(request); 타입/반환형은 개별 원문 확인 필요 | controller/ItemInventoryController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getInventoryList | ItemInventoryController | 설계 p.6·8·34~35 | getInventoryList(); 타입/반환형은 개별 원문 확인 필요 | controller/ItemInventoryController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateInventory | ItemInventoryController | 설계 p.6·8·34~35 | updateInventory(inventoryId, quantity); 타입/반환형은 개별 원문 확인 필요 | controller/ItemInventoryController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getInventory | ItemInventoryService | 설계 p.6·9·34~35 | getInventory(); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | registerItem | ItemInventoryService | 설계 p.6·9·34~35 | registerItem(request); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateQuantity | ItemInventoryService | 설계 p.6·9·34~35 | updateQuantity(itemId, quantity); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateStockStatus | ItemInventoryService | 설계 p.6·9·34~35 | updateStockStatus(); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | registerInventory | ItemInventoryService | 설계 p.6·9·34~35 | registerInventory(request); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | getInventoryList | ItemInventoryService | 설계 p.6·9·34~35 | getInventoryList(); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | updateInventory | ItemInventoryService | 설계 p.6·9·34~35 | updateInventory(inventoryId, quantity); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateQuantity | ItemInventoryService | 설계 p.6·9·34~35 | validateQuantity(quantity); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | determineInventoryStatus | ItemInventoryService | 설계 p.6·9·34~35 | determineInventoryStatus(quantity); 타입/반환형은 개별 원문 확인 필요 | service/ItemInventoryService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | sendDepartureNotifications | SmsNotificationService | 설계 p.6·10·27·39 | sendDepartureNotifications(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/SmsNotificationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | recordDeliveryResult | SmsNotificationService | 설계 p.6·10·27·39 | recordDeliveryResult(); 타입/반환형은 개별 원문 확인 필요 | service/SmsNotificationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | createNotification | SmsNotificationService | 설계 p.6·10·27·39 | createNotification(application); 타입/반환형은 개별 원문 확인 필요 | service/SmsNotificationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | handleDepartureConfirmed | SmsNotificationService | 설계 p.6·10·27·39 | handleDepartureConfirmed(scheduleId); 타입/반환형은 개별 원문 확인 필요 | service/SmsNotificationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | upDateStatus | SmsNotificationService | 설계 p.6·10·27·39 | upDateStatus(); 타입/반환형은 개별 원문 확인 필요 | service/SmsNotificationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | retryFailedNotifications | SmsNotificationService | 설계 p.6·10·27·39 | retryFailedNotifications(); 타입/반환형은 개별 원문 확인 필요 | service/SmsNotificationService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | interpret | VoiceController | 설계 p.6·8·21~22 | interpret(request); 타입/반환형은 개별 원문 확인 필요 | controller/VoiceController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | interpretVoice | VoiceController | 설계 p.6·8·21~22 | interpretVoice(text, context); 타입/반환형은 개별 원문 확인 필요 | controller/VoiceController.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | interpret | VoiceService | 설계 p.6·9·21~22 | interpret(request); 타입/반환형은 개별 원문 확인 필요 | service/VoiceService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | interpretVoice | VoiceService | 설계 p.6·9·21~22 | interpretVoice(text, context); 타입/반환형은 개별 원문 확인 필요 | service/VoiceService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | matchSelection | VoiceService | 설계 p.6·9·21~22 | matchSelection(request); 타입/반환형은 개별 원문 확인 필요 | service/VoiceService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | validateInference | VoiceService | 설계 p.6·9·21~22 | validateInference(result); 타입/반환형은 개별 원문 확인 필요 | service/VoiceService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | evaluateInferenceFailures | VoiceService | 설계 p.6·9·21~22 | evaluateInferenceFailures(); 타입/반환형은 개별 원문 확인 필요 | service/VoiceService.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | transcribe | VoiceProcessingClient | 설계 p.6·21 | transcribe(); 타입/반환형은 개별 원문 확인 필요 | service/VoiceProcessingClient.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | interpret | VoiceProcessingClient | 설계 p.6·21 | interpret(request); 타입/반환형은 개별 원문 확인 필요 | service/VoiceProcessingClient.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | interpret | VoiceProcessingClient | 설계 p.6·21 | interpret(text, context); 타입/반환형은 개별 원문 확인 필요 | service/VoiceProcessingClient.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |
| 구현 메서드 | sendSms | SmsClient | 설계 p.13·27 | sendSms(phoneNumber, message); 타입/반환형은 개별 원문 확인 필요 | service/SmsClient.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |

## 그림·본문 충돌 (검토 확인)

- p.19 그림 `buildScheduleResponse(schedules)` / p.20 본문 `buildScheduleSelectionResponse(schedules)`.
- p.23 그림 `updateApplicantCount(scheduleId, applicantCount)` / p.24 본문 `updateApplicantCount(scheduleId)`.
- p.25 그림 `requestDepartureNotification(scheduleId)` / p.26 본문 `sendDepartureNotifications(scheduleId)`.
- p.27 그림 `savePendingNotification(application)` / 본문 `createNotification(application)`.
- p.33 그림 `buildReservationStatusResponse(schedule, applications)` / 본문 `calculateReservationStatus(schedule, applications)`.
- p.6 `ApplicationResponse` / p.24 `TravelApplicationResponse`; p.6 `ScheduleResponse` / p.33 `ReservationStatusResponse`: 반환형 결정 질문 대기.
- p.6 `calculateFinalPrice` / p.23 `calculatePrice`, `calculateDiscount` / `applyLoyaltyDiscount`.
- p.27 본문 `upDateStatus()` / 그림·메시지 설명 `updateStatus(notificationId, SENT/FAILED)`.

| 종류 | 원문 이름 | 소속 클래스·문맥 | 문서·페이지 | 명시된 타입·매개변수 | 구현 위치 | 충돌·비고 |
|---|---|---|---|---|---|---|
| 응답 메시지 | ResponseBookingCompletion | Customer ← System | 분석 p.14 그림 | 미명시 | Java 메서드 생성 안 함 | 신청 완료 응답 |
| 화면 조작 | selectGradeAndDefaultOptions | Customer | 설계 p.19 | () | UI | 저장 API 아님 |
| 화면 조작 | confirmValidSelection | Customer | 설계 p.22 | () | UI | 신청 생성 아님 |
| 일반 응답 변수 | savedInventory | 재고 저장 결과 | 설계 p.35 | 미명시 | ItemInventoryService 로컬 값 | DTO 클래스명 아님 |
| 이벤트·조건 | applicationDeadlinePassed | TravelSchedule 상태도 | 설계 p.36~37 | 미명시 | 미구현: 마감 정책 대기 | Java 메서드로 강제하지 않음 |
| 이벤트·액션 | closeRecruitment / startTravel / completeTravel | TravelSchedule 상태도 | 설계 p.36~37 | () | 전이 정책 대기 | 상태도 액션 |
| 외부 이벤트 | AfterCommitEvent | 알림 발송 시작 | 설계 p.27 그림 | 미명시 | 미구현 | 영속 알림을 업무 커밋 전에 저장하는 보강 필요 |

## DTO·공통 객체·외부 연동 원문 이름

아래 이름은 본문·클래스도에 실제 존재한다. DTO 필드 전체·반환형 전체의 일치 검증은 미완료이며, 같은 책임의 다른 이름을 삭제하지 않는다.

| 종류 | 원문 이름 | 소속 클래스·문맥 | 문서·페이지 | 명시된 타입·매개변수 | 구현 위치 | 충돌·비고 |
|---|---|---|---|---|---|---|
| DTO | SignUpRequest | 기능별 요청·응답 | 설계 p.6·8 | 필드 전체 미명시 또는 미대조 | dto/SignUpRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | SignUpResponse | 기능별 요청·응답 | 설계 p.6·8 | 필드 전체 미명시 또는 미대조 | dto/SignUpResponse.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | LoginRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/LoginRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | LoginResponse | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/LoginResponse.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | MemberUpdateRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/MemberUpdateRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | MemberResponse | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/MemberResponse.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelProductRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/TravelProductRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelOptionRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/TravelOptionRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelScheduleRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/TravelScheduleRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelApplicationRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | 미구현 | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | ApplicationResponse | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/ApplicationResponse.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelApplicationResponse | 기능별 요청·응답 | 설계 p.24 | 필드 전체 미명시 또는 미대조 | 미구현 | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelScheduleResponse | 기능별 요청·응답 | 설계 p.20 | 필드 전체 미명시 또는 미대조 | dto/TravelScheduleResponse.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | ScheduleResponse | 기능별 요청·응답 | 설계 p.6·32 | 필드 전체 미명시 또는 미대조 | 미구현 | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | ReservationStatusResponse | 기능별 요청·응답 | 설계 p.33 | 필드 전체 미명시 또는 미대조 | 미구현 | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | TravelHistoryResponse | 기능별 요청·응답 | 설계 p.28 | 필드 전체 미명시 또는 미대조 | 미구현 | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | ItemInventoryRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/ItemInventoryRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | InventoryResponse | 기능별 요청·응답 | 설계 p.35 | 필드 전체 미명시 또는 미대조 | dto/InventoryResponse.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | InventoryListResponse | 기능별 요청·응답 | 설계 p.35 | 필드 전체 미명시 또는 미대조 | 미구현 | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | VoiceCommandRequest | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/VoiceCommandRequest.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | VoiceSelectionResult | 기능별 요청·응답 | 설계 p.6 | 필드 전체 미명시 또는 미대조 | dto/VoiceSelectionResult.java | 없는 서명을 문서 명시로 간주하지 않음 |
| DTO | ResultDto | 기능별 요청·응답 | 설계 p.13 | 필드 전체 미명시 또는 미대조 | common/ResultDto.java | 없는 서명을 문서 명시로 간주하지 않음 |
| 인터페이스 | MemberRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/MemberRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | TravelProductRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/TravelProductRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | TravelOptionRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/TravelOptionRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | TravelScheduleRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/TravelScheduleRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | TravelApplicationRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/TravelApplicationRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | TravelParticipantRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/TravelParticipantRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | ItemInventoryRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/ItemInventoryRepository.java | Spring Data 상속 메서드 포함 |
| 인터페이스 | SmsNotificationRepository | Repository | 설계 p.6·11 | 그림·본문 | repository/SmsNotificationRepository.java | Spring Data 상속 메서드 포함 |
| 인증·공통 | SecurityConfig | 보안·예외 | 설계 p.13 | 미명시 | config / security / common | PasswordEncoder는 Spring 인터페이스 Bean |
| 인증·공통 | JwtProvider | 보안·예외 | 설계 p.13 | 미명시 | config / security / common | PasswordEncoder는 Spring 인터페이스 Bean |
| 인증·공통 | JwtAuthenticationFilter | 보안·예외 | 설계 p.13 | 미명시 | config / security / common | PasswordEncoder는 Spring 인터페이스 Bean |
| 인증·공통 | GlobalExceptionHandler | 보안·예외 | 설계 p.13 | 미명시 | config / security / common | PasswordEncoder는 Spring 인터페이스 Bean |
| 인증·공통 | PasswordEncoder | 보안·예외 | 설계 p.13 | 미명시 | config / security / common | PasswordEncoder는 Spring 인터페이스 Bean |

## 구현상 추가 사항

- Controller의 Entity 반환을 DTO로 바꾸라는 사용자 보안 요구에 따라 반환형은 DTO로 분리했다. 기존 이름을 새 이름으로 치환한 것으로 간주하지 않는다. 클래스도 서명 그대로와는 차이가 있으며 전체 호환 계층은 미완료다.
- Page/Pageable, HTTP Authentication 인수는 목록 크기 제한 및 인증 식별자에 필요한 추가 매개변수다. 클래스도 무인수 getInventory/getProducts 등의 정확한 서명 보존은 아직 미완료다.
- 관계 필드 travelProduct/member/travelSchedule/travelApplication은 p.6 관계를 구현하기 위한 필드명 추가다.
- NotificationType은 p.39 알림 종류를 표현하는 추가 Enum 타입명이다. ParticipantType 타입은 p.6에 있고 APPLICANT/COMPANION 상수는 신청자·동행자 구분을 구현하기 위한 추가 표기다.
- TravelProductResponse/TravelOptionResponse/NotificationResponse, Pages, BusinessException, TimeConfig, TokenSession/TokenSessionRepository, UnavailableVoiceProcessingClient/UnavailableSmsClient, VoiceInterpretation/InferenceState는 입력·응답 분리, 공통 검증, 회수, 외부 미연결 표현을 위한 추가 설계다.
- REFRESH 세션 회수 테이블은 JWT 로그아웃과 재사용 방지를 위한 추가 설계이며 원문 DB 테이블이 아니다.
- API에서 CLASSIC/GRAND/PREMIUM, HOTEL/TRANSPORT/MEAL/EXTRA는 String 계약의 추가 표기다. PDF에 이 이름의 Enum 타입이 있다고 주장하지 않는다.


## Lombok 생성 코드

기존 getter/setter 및 단순 의존성 주입 생성자는 Lombok으로 생성한다. 원문 필드명과 접근 메서드명은 그대로 유지한다. `@Getter`는 클래스에, `@Setter`는 기존 setter가 있던 필드에만 적용한다. `@RequiredArgsConstructor`는 final 필드 선언 순서대로 기존 주입 생성자를 대체한다. 검증·업무 로직이 있는 생성자와 메서드는 직접 구현을 유지한다.


## 2026-11-03 사용자 결정 반영

`TravelApplicationService.applyLoyaltyDiscount(completedTrips)`를 할인율 판정용으로 구현했다. 메서드·인수명은 설계 p.23 표기를 사용한다. `long` 인수·`int` 할인율 반환은 구현 선택이며 PDF에 그 타입이 명시되었다고 주장하지 않는다. 금액 계산·반올림·완료 횟수 집계와의 연결은 아직 미구현이다. 사용자 결정으로 변경된 재고 부족 기준 3개는 PDF 원문 명칭/본문을 수정한 것이 아니다.

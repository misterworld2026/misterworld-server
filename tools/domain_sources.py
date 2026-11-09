"""Reviewed PDF p.6 fields. Also writes the naming register BEFORE generating code.

Run from project root. Korean SQL identifiers follow the prompt; actual DDL absent.
"""
from pathlib import Path

base = Path('src/main/java/com/misterworld/server')
docs = Path('docs'); docs.mkdir(exist_ok=True)
definitions = {
 'Member': ('회원', [('id','Long','회원_ID'),('email','String','이메일'),('password','String','비밀번호'),('name','String','이름'),('address','String','주소'),('phoneNumber','String','연락처'),('travelCount','Integer','Field'),('role','Role','회원구분')]),
 'TravelProduct': ('여행상품', [('id','Long','여행상품_ID'),('name','String','상품명'),('theme','String','테마'),('description','String','상품설명'),('classicPrice','Integer','클래식_가격'),('grandPrice','Integer','그랜드_가격'),('premiumPrice','Integer','프리미엄_가격')]),
 'TravelOption': ('여행옵션', [('id','Long','여행옵션_ID'),('travelProduct','TravelProduct','여행상품_ID'),('type','String','옵션종류'),('name','String','옵션명'),('grade','String','옵션등급'),('additionalPrice','Integer','추가금액'),('description','String','옵션설명'),('defaultForGrade','String','기본등급')]),
 'TravelSchedule': ('여행일정', [('travelScheduleId','Long','여행일정_ID'),('travelProduct','TravelProduct','여행상품_ID'),('departureDate','LocalDate','출발일'),('arrivalDate','LocalDate','종료일'),('status','ScheduleStatus','여행상태'),('applicantCount','Integer','현재신청인원')]),
 'TravelApplication': ('여행신청', [('id','Long','여행신청_ID'),('member','Member','회원_ID'),('travelSchedule','TravelSchedule','여행일정_ID'),('tourGrade','String','투어등급'),('hotelOption','String','호텔옵션'),('transportOption','String','교통옵션'),('mealOption','String','식사옵션'),('extraOption','String','추가옵션'),('applicantCount','Integer','신청인원'),('basePrice','Integer','기본가격'),('additionalOptionPrice','Integer','옵션추가금액'),('discountRate','Integer','할인율'),('discountAmount','Integer','할인금액'),('totalPrice','Integer','최종가격'),('status','ApplicationStatus','신청상태'),('appliedAt','LocalDateTime','신청일시')]),
 'TravelParticipant': ('여행객', [('id','Long','여행객_ID'),('travelApplication','TravelApplication','여행신청_ID'),('name','String','이름'),('phoneNumber','String','연락처'),('birthDate','LocalDate','생년월일'),('participantType','ParticipantType','참가자구분')]),
 'ItemInventory': ('물품재고', [('id','Long','물품재고_ID'),('travelProduct','TravelProduct','여행상품_ID'),('itemName','String','물품명'),('quantity','Integer','재고수량'),('status','InventoryStatus','재고상태'),('updatedAt','LocalDateTime','수정일시')]),
 'SmsNotification': ('문자알림', [('id','Long','문자알림_ID'),('travelApplication','TravelApplication','여행신청_ID'),('notificationType','NotificationType','알림종류'),('recipientNumber','String','수신번호'),('message','String','메시지내용'),('status','NotificationStatus','발송상태'),('sentAt','LocalDateTime','발송일시')])
}
enums = {'Role':['MEMBER','STAFF'], 'ScheduleStatus':['RECRUITING_SCHEDULE','CONFIRMED_SCHEDULE','CLOSED_SCHEDULE','IN_PROGRESS_SCHEDULE','COMPLETED_SCHEDULE','CANCELLED_SCHEDULE'], 'ApplicationStatus':['PENDING','CONFIRMED','CANCELLED'], 'InventoryStatus':['NORMAL','LOW_STOCK','OUT_OF_STOCK'], 'NotificationStatus':['PENDING','SENT','FAILED'], 'NotificationType':['DEPARTURE_CONFIRMED','SCHEDULE_CANCELLED'], 'ParticipantType':['APPLICANT','COMPANION']}
rows = ['# 원문 명칭 대장 (검토 진행 중)', '', '이 대장은 확인한 선언만 수록한다. 전체 PDF의 모든 메시지·조건·액션을 수록한 완성본이 아니다. 타입 미명시 부분은 추가 설계로 구분한다. 구현 위치가 있어도 동작 완료를 뜻하지 않는다.', '', '| 종류 | 원문 이름 | 소속 클래스·문맥 | 문서·페이지 | 명시된 타입·매개변수 | 구현 위치 | 충돌·비고 |','|---|---|---|---|---|---|---|']
for cls,(table,fields) in definitions.items():
 rows.append(f'| Entity | {cls} | {cls} | 설계 p.6·12 | 클래스 | domain/{cls}.java | `{table}` 매핑 |')
 for name,typ,col in fields:
  note=''
  if typ in definitions: note='관계는 p.6·12에 명시, Java 관계 필드명은 추가 설계'
  if name=='notificationType': note='p.39 명시, 타입명 NotificationType은 추가 설계'
  rows.append(f'| 필드 | {name} | {cls} | 설계 p.6'+('·39' if name=='notificationType' else '')+f' | {typ} | domain/{cls}.java#{name} | DB `{col}`; {note} |')
for typ,values in enums.items():
 rows.append(f'| Enum | {typ} | domain | 설계 p.6·12 | {", ".join(values)} | domain/{typ}.java | '+('타입명·상수명은 추가 설계' if typ in ['ParticipantType','NotificationType'] else 'ParticipantType 타입명은 p.6, 상수는 추가 설계' if typ=='ParticipantType' else '상태 상수는 p.12·36~39')+' |')
methods={
 'AuthController':['signup(request)','login(request)','logout()'], 'AdminAuthController':['signup(request)'],
 'AuthService':['signup(request)','createStaffAccount(request)','login(request)','logout()','validatePasswordConfirmation(request)'],
 'MemberController':['getMyInfo(memberId)','updateMyInfo(memberId, request)'], 'MemberService':['getMyInfo(memberId)','updateMyInfo(memberId, request)'],
 'TravelProductController':['getProducts()','getProducts(theme)','getProduct(productId)','getOptions(productId)','createProduct(request)','updateProduct(request)','saveOption(request)','saveProduct(request)'],
 'TravelProductService':['getProducts()','getProducts(theme)','getProduct(productId)','getOptions(productId)','saveProduct(request)','saveOption(request)','validateSelection(productId, grade, optionIds)','buildAllowedGradesAndDefaults()','prepareNewProduct()','validateThemeGradesPricesAndOptions()'],
 'TravelScheduleController':['getSchedules(productId)','createSchedule(request)','updateSchedule(request)','getReservationStatus(scheduleId)','saveSchedule(request)'],
 'TravelScheduleService':['getSchedules(productId)','saveSchedule(request)','getReservationStatus(scheduleId)','updateApplicantCount(scheduleId)','confirmDeparture(scheduleId)','completeTravel(scheduleId)','validateAndLockSchedule(scheduleId, applicantCount)','buildScheduleSelectionResponse(schedules)','validateDates(departureDate, endDate)','validateScheduleChange(schedule, request, hasApplications)','checkDepartureCondition(scheduleId)','calculateValidApplicantCount(applications)','isDepartureConditionMet(schedule, applications)','calculateReservationStatus(schedule, applications)'],
 'TravelApplicationController':['apply(request)','getMyApplication(memberId)','getApplication(applicationId)','getTravelHistory(memberId)'],
 'TravelApplicationService':['apply(request)','apply(memberId, request)','getMyApplication(memberId)','getApplication(applicationId)','getTravelHistory(memberId)','calculateFinalPrice(request)','calculateDiscount(request)','calculatePrice(selection, applicantCount)','applyLoyaltyDiscount(completedTrips)','buildTravelHistoryResponse(applications)'],
 'ItemInventoryController':['getInventory()','registerItem(request)','updateQuantity(itemId, quantity)','registerInventory(request)','getInventoryList()','updateInventory(inventoryId, quantity)'],
 'ItemInventoryService':['getInventory()','registerItem(request)','updateQuantity(itemId, quantity)','updateStockStatus()','registerInventory(request)','getInventoryList()','updateInventory(inventoryId, quantity)','validateQuantity(quantity)','determineInventoryStatus(quantity)'],
 'SmsNotificationService':['sendDepartureNotifications(scheduleId)','recordDeliveryResult()','createNotification(application)','handleDepartureConfirmed(scheduleId)','upDateStatus()','retryFailedNotifications()'],
 'VoiceController':['interpret(request)','interpretVoice(text, context)'], 'VoiceService':['interpret(request)','interpretVoice(text, context)','matchSelection(request)','validateInference(result)','evaluateInferenceFailures()'],
 'VoiceProcessingClient':['transcribe()','interpret(request)','interpret(text, context)'], 'SmsClient':['sendSms(phoneNumber, message)']}
for owner,names in methods.items():
 for name in names:
  rows.append(f'| メソッド / 구현 메서드 | {name.split("(")[0]} | {owner} | 설계 p.6 또는 해당 4장 본문 (아래 비고) | {name}; 타입/반환형은 개별 원문 확인 필요 | {"controller" if owner.endswith("Controller") else "service"}/{owner}.java | 대장 전체 대조 미완료; 없는 구현은 미구현 |')
rows += ['', '## 그림·본문 충돌 (검토 확인)', '', '- p.19 그림 `buildScheduleResponse(schedules)` / p.20 본문 `buildScheduleSelectionResponse(schedules)`.', '- p.23 그림 `updateApplicantCount(scheduleId, applicantCount)` / p.24 본문 `updateApplicantCount(scheduleId)`.', '- p.25 그림 `requestDepartureNotification(scheduleId)` / p.26 본문 `sendDepartureNotifications(scheduleId)`.', '- p.27 그림 `savePendingNotification(application)` / 본문 `createNotification(application)`.', '- p.33 그림 `buildReservationStatusResponse(schedule, applications)` / 본문 `calculateReservationStatus(schedule, applications)`.', '- p.6 `ApplicationResponse` / p.24 `TravelApplicationResponse`; p.6 `ScheduleResponse` / p.33 `ReservationStatusResponse`: 반환형 결정 질문 대기.', '- p.6 `calculateFinalPrice` / p.23 `calculatePrice`, `calculateDiscount` / `applyLoyaltyDiscount`.', '- p.27 본문 `upDateStatus()` / 그림·메시지 설명 `updateStatus(notificationId, SENT/FAILED)`.', '', '| 종류 | 원문 이름 | 소속 클래스·문맥 | 문서·페이지 | 명시된 타입·매개변수 | 구현 위치 | 충돌·비고 |','|---|---|---|---|---|---|---|', '| 응답 메시지 | ResponseBookingCompletion | Customer ← System | 분석 p.14 그림 | 미명시 | Java 메서드 생성 안 함 | 신청 완료 응답 |', '| 화면 조작 | selectGradeAndDefaultOptions | Customer | 설계 p.19 | () | UI | 저장 API 아님 |', '| 화면 조작 | confirmValidSelection | Customer | 설계 p.22 | () | UI | 신청 생성 아님 |', '| 일반 응답 변수 | savedInventory | 재고 저장 결과 | 설계 p.35 | 미명시 | ItemInventoryService 로컬 값 | DTO 클래스명 아님 |', '| 이벤트·조건 | applicationDeadlinePassed | TravelSchedule 상태도 | 설계 p.36~37 | 미명시 | 미구현: 마감 정책 대기 | Java 메서드로 강제하지 않음 |', '| 이벤트·액션 | closeRecruitment / startTravel / completeTravel | TravelSchedule 상태도 | 설계 p.36~37 | () | 전이 정책 대기 | 상태도 액션 |', '| 외부 이벤트 | AfterCommitEvent | 알림 발송 시작 | 설계 p.27 그림 | 미명시 | 미구현 | 영속 알림을 업무 커밋 전에 저장하는 보강 필요 |']
(docs/'NAME_REGISTER.md').write_text('\n'.join(rows)+'\n',encoding='utf-8')

def write(rel,text):
 p=base/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
for typ,values in enums.items(): write(f'domain/{typ}.java',f'package com.misterworld.server.domain;\npublic enum {typ} {{ {", ".join(values)} }}\n')
sql=[]
for cls,(table,fields) in definitions.items():
 body=['package com.misterworld.server.domain;','', 'import jakarta.persistence.*;','import java.time.*;','import lombok.Getter;','import lombok.Setter;','', '@Getter', '@Entity',f'@Table(name = "{table}")',f'public class {cls} {{']
 columns=[]; constraints=[]
 for idx,(name,typ,col) in enumerate(fields):
  nullable=name in ['classicPrice','sentAt','description','defaultForGrade','extraOption']
  annotation=f'    @Column(name = "{col}", nullable = {str(nullable).lower()}'
  if idx==0:
   body+=['    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)']; annotation+=')'; ddl='BIGINT AUTO_INCREMENT PRIMARY KEY'
  elif typ in definitions:
   body+=['    @ManyToOne(fetch = FetchType.LAZY, optional = false)',f'    @JoinColumn(name = "{col}", nullable = false)']; annotation=''
   target=definitions[typ]; targetpk=target[1][0][2]; ddl='BIGINT NOT NULL'
   constraints.append(f'FOREIGN KEY (`{col}`) REFERENCES `{target[0]}` (`{targetpk}`)')
  elif typ in enums:
   body+=['    @Enumerated(EnumType.STRING)']; annotation+=', length = 40)'; ddl='VARCHAR(40) NOT NULL'
   constraints.append(f'CHECK (`{col}` IN ('+', '.join("'"+v+"'" for v in enums[typ])+'))')
  elif typ=='Integer':
   annotation+=')'; ddl='INT'+('' if nullable else ' NOT NULL'); constraints.append(f'CHECK (`{col}` >= 0)')
  elif typ in ['LocalDate','LocalDateTime']:
   annotation+=')'; ddl=('DATE' if typ=='LocalDate' else 'DATETIME(6)')+('' if nullable else ' NOT NULL')
  else:
   length=2000 if name in ['description','extraOption','message'] else 255
   annotation+=f', length = {length})'; ddl=f'VARCHAR({length})'+('' if nullable else ' NOT NULL')
  if annotation: body.append(annotation)
  if idx: body.append('    @Setter')
  body.append(f'    private {typ} {name};')
  columns.append(f'`{col}` {ddl}')

 if cls=='Member':
  constraints.append('UNIQUE (`이메일`)');body.append('    public void increaseTravelCount() { travelCount = Math.addExact(travelCount, 1); }')
 if cls=='TravelSchedule':constraints.append('CHECK (`종료일` >= `출발일`)')
 if cls=='TravelApplication':constraints.append('CHECK (`신청인원` > 0)')
 if cls=='SmsNotification':constraints.append('UNIQUE (`여행신청_ID`, `알림종류`)')
 body.append('}')
 write(f'domain/{cls}.java','\n'.join(body)+'\n')
 sql.append(f'CREATE TABLE `{table}` (\n    '+',\n    '.join(columns+constraints)+'\n);')
 Path('src/main/resources/db/migration').mkdir(parents=True,exist_ok=True)
Path('src/main/resources/db/migration/V1__empty_database.sql').write_text('-- Empty database ONLY. Existing DDL/data must be reviewed before migration.\n'+'\n\n'.join(sql)+'\n',encoding='utf-8')

package com.misterworld.server;

import com.jayway.jsonpath.JsonPath;
import com.misterworld.server.member.entity.*;
import com.misterworld.server.member.entity.Role;
import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.security.*;
import com.misterworld.server.domain.*;
import com.misterworld.server.repository.*;
import com.misterworld.server.travelapplication.entity.*;
import com.misterworld.server.travelapplication.repository.*;
import com.misterworld.server.domain.*;
import com.misterworld.server.repository.TravelScheduleRepository;
import com.misterworld.server.travelschedule.service.ScheduleStatusScheduler;
import com.misterworld.server.notification.entity.*;
import com.misterworld.server.notification.repository.SmsNotificationRepository;
import com.misterworld.server.notification.service.*;
import com.misterworld.server.notification.integration.SmsClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "app.booking.pricing-unit=PER_PERSON_SURCHARGE","app.booking.discount-rounding=HALF_UP",
    "app.booking.zone=Asia/Seoul","app.booking.honeymoon-size=ONE_TEAM",
    "app.booking.cancel-after-deadline=true","app.sms.dispatch-enabled=false","app.sms.timeout-ms=100",
    "app.schedule.transition-cron=-"})
@ActiveProfiles("local")
@Import(BookingManagementIntegrationTests.TestConfig.class)
class BookingManagementIntegrationTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getenv("MW_TEST_DB_URL");
        r.add("spring.datasource.url", () -> url==null ? "jdbc:h2:mem:booking;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=FALSE" : url);
        r.add("spring.datasource.username", () -> url==null ? "sa" : "root");
        r.add("spring.datasource.password", () -> url==null ? "" : System.getenv("MW_TEST_DB_PASSWORD"));
    }
    static class MutableClock extends Clock {
        final AtomicReference<Instant> value=new AtomicReference<>(Instant.parse("2026-11-04T00:00:00Z"));
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(value.get(),zone);}
        public Instant instant(){return value.get();}
    }
    static class TestSms implements SmsClient {
        final AtomicInteger count=new AtomicInteger(); volatile String mode="SUCCESS";
        volatile boolean safeRetry = true;
        final Set<String> deliveredKeys=ConcurrentHashMap.newKeySet();
        public boolean supportsSafeRetry() { return safeRetry; }
        public void sendSms(String phoneNumber,String message,String deliveryKey) {
            if(deliveredKeys.contains(deliveryKey))return;
            sendSms(phoneNumber,message);
            deliveredKeys.add(deliveryKey);
        }
        public void sendSms(String phoneNumber,String message) {
            count.incrementAndGet();
            if(mode.equals("REJECTED")) throw new SmsClient.Rejected("test provider rejected request");
            if(mode.equals("FAIL")) throw new IllegalStateException("test provider failure");
            if(mode.equals("TIMEOUT")) try {Thread.sleep(1000);} catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }
    @TestConfiguration static class TestConfig {
        @Bean @Primary MutableClock testClock(){return new MutableClock();}
        @Bean @Primary TestSms testSms(){return new TestSms();}
    }
    @LocalServerPort int port;
    @Autowired MemberRepository members;
    @Autowired TravelProductRepository products;
    @Autowired TravelOptionRepository options;
    @Autowired TravelScheduleRepository schedules;
    @Autowired ScheduleStatusScheduler scheduleStatusScheduler;
    @Autowired TravelApplicationRepository applications;
    @Autowired @org.springframework.test.context.bean.override.mockito.MockitoSpyBean TravelParticipantRepository participants;
    @Autowired TokenSessionRepository sessions;
    @Autowired JwtProvider jwt;
    @Autowired SmsNotificationRepository notifications;
    @Autowired SmsNotificationService sms;
    @Autowired SmsDeliveryStore deliveryStore;
    @Autowired MutableClock clock;
    @Autowired TestSms provider;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private final HttpClient http=HttpClient.newHttpClient();
    private Member customer,staff;
    private String token,staffToken;
    private TravelProduct product;
    private TravelSchedule schedule;
    private List<Long> selected;

    @BeforeEach void setup() {
        clock.value.set(Instant.parse("2026-11-04T00:00:00Z")); provider.mode="SUCCESS"; provider.safeRetry=true;
        customer=member(Role.MEMBER);staff=member(Role.STAFF);token=token(customer);staffToken=token(staff);
        product=product("골프"); schedule=schedule(product,LocalDate.of(2026,12,4));selected=optionIds(product,"CLASSIC");
    }
    Member member(Role role) {
        var m=new Member();m.setEmail(UUID.randomUUID()+"@example.com");m.setPassword("fixture-hash");m.setName("고객");
        m.setAddress("서울");m.setPhoneNumber("01012345678");m.setTravelCount(99);m.setRole(role);return members.saveAndFlush(m);
    }
    String token(Member m) {
        String sid=UUID.randomUUID().toString();sessions.saveAndFlush(new TokenSession(sid,m.getId(),"fixture",clock.instant().plusSeconds(86400*365)));
        return jwt.generateAccessToken(m,sid);
    }
    TravelProduct product(String theme) {
        var p=new TravelProduct();p.setName("상품-"+UUID.randomUUID());p.setTheme(theme);
        p.setClassicPrice(Set.of("허니문","효도").contains(theme)?null:101);p.setGrandPrice(201);p.setPremiumPrice(301);
        products.saveAndFlush(p);
        for(String grade:List.of("CLASSIC","GRAND","PREMIUM")) {
            if(p.getClassicPrice()==null && grade.equals("CLASSIC"))continue;
            for(String type:List.of("HOTEL","TRANSPORT","MEAL")) {
                var o=new TravelOption();o.setTravelProduct(p);o.setName(grade+" "+type);o.setGrade(grade);o.setType(type);
                o.setDefaultForGrade(grade);o.setAdditionalPrice(0);options.saveAndFlush(o);
            }
        }
        return p;
    }
    List<Long> optionIds(TravelProduct p,String grade) {
        return options.findByTravelProductId(p.getId()).stream().filter(o -> grade.equals(o.getDefaultForGrade()))
            .sorted(Comparator.comparing(TravelOption::getType)).map(TravelOption::getId).toList();
    }
    TravelSchedule schedule(TravelProduct p,LocalDate date) {
        var s=new TravelSchedule();s.setTravelProduct(p);s.setDepartureDate(date);s.setArrivalDate(date.plusDays(3));
        s.setApplicantCount(0);s.setStatus(ScheduleStatus.RECRUITING_SCHEDULE);return schedules.saveAndFlush(s);
    }
    HttpResponse<String> call(String method,String path,String body,String access) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");
        if(access!=null)b.header("Authorization","Bearer "+access);
        return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    String body(int count,String key,String quote) {
        var people=new ArrayList<String>();
        for(int i=0;i<count;i++)people.add("{\"name\":\"여행객"+i+"\",\"phoneNumber\":\"01012345678\",\"birthDate\":\"1990-01-01\",\"participantType\":\""+(i==0?"APPLICANT":"COMPANION")+"\"}");
        return "{\"productId\":"+product.getId()+",\"scheduleId\":"+schedule.getTravelScheduleId()+",\"grade\":\""+(product.getClassicPrice()==null?"GRAND":"CLASSIC")+"\",\"optionIds\":"+selected+
            ",\"applicantCount\":"+count+",\"participants\":["+String.join(",",people)+"],\"requestKey\":\""+key+"\""+(quote==null?"":",\"quoteToken\":\""+quote+"\"")+"}";
    }
    String quote(int count,String key) throws Exception {
        var r=call("POST","/api/reservations/quote",body(count,key,null),token);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);return JsonPath.read(r.body(),"$.quoteToken");
    }
    HttpResponse<String> book(int count,String key) throws Exception {
        return call("POST","/api/reservations",body(count,key,quote(count,key)),token);
    }
    long id(HttpResponse<String> r) {assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return ((Number)JsonPath.read(r.body(),"$.application.id")).longValue();}
    void error(HttpResponse<String> r,int status,String code) {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(status);
        assertThat((Number)JsonPath.read(r.body(),"$.status")).isEqualTo(status);assertThat((String)JsonPath.read(r.body(),"$.code")).isEqualTo(code);
    }
    List<TravelApplication> valid() {return applications.findValidApplicationsByScheduleId(schedule.getTravelScheduleId());}
    List<SmsNotification> messages(long applicationId) {
        return notifications.findAll().stream().filter(n -> n.getTravelApplication().getId()==applicationId).toList();
    }
    TravelApplication completed(Member m,TravelSchedule s) {
        var a=new TravelApplication();a.setMember(m);a.setTravelSchedule(s);a.setTourGrade("CLASSIC");a.setHotelOption("이전 호텔");
        a.setTransportOption("이전 교통");a.setMealOption("이전 식사");a.setApplicantCount(1);a.setBasePrice(100);a.setAdditionalOptionPrice(0);
        a.setDiscountRate(0);a.setDiscountAmount(0);a.setTotalPrice(100);a.setStatus(ApplicationStatus.CONFIRMED);a.setAppliedAt(LocalDateTime.of(2025,1,1,0,0));
        return applications.saveAndFlush(a);
    }

    @Test void mockPaymentUsesStoredAmountAndIsIdempotentWithoutDepartureEffects() throws Exception {
        long applicationId=id(book(2,UUID.randomUUID().toString()));
        String path="/api/my/applications/"+applicationId+"/payment";
        long notificationCount=notifications.count(), participantCount=participants.count();
        var paid=call("POST",path,"{\"totalPrice\":1}",token);
        assertThat(paid.statusCode()).as(paid.body()).isEqualTo(200);
        assertThat((String)JsonPath.read(paid.body(),"$.paymentStatus")).isEqualTo("PAID");
        assertThat((Number)JsonPath.read(paid.body(),"$.paidAmount")).isEqualTo(202);
        assertThat((String)JsonPath.read(paid.body(),"$.status")).isEqualTo("PENDING");
        assertThat(call("POST",path,null,token).body()).isEqualTo(paid.body());
        assertThat(notifications.count()).isEqualTo(notificationCount);
        assertThat(participants.count()).isEqualTo(participantCount);
        var unchanged=schedules.findById(schedule.getTravelScheduleId()).orElseThrow();
        assertThat(unchanged.getApplicantCount()).isEqualTo(2);
        assertThat(unchanged.getStatus()).isEqualTo(ScheduleStatus.RECRUITING_SCHEDULE);
        var detail=call("GET","/api/my/applications/"+applicationId,null,token);
        assertThat((String)JsonPath.read(detail.body(),"$.paymentStatus")).isEqualTo("PAID");
    }

    @Test void mockPaymentEnforcesAuthenticationOwnershipAndCancellation() throws Exception {
        long applicationId=id(book(1,UUID.randomUUID().toString()));
        String path="/api/my/applications/"+applicationId+"/payment";
        error(call("POST",path,null,null),401,"UNAUTHORIZED");
        error(call("POST",path,null,staffToken),403,"FORBIDDEN");
        error(call("POST",path,null,token(member(Role.MEMBER))),404,"NOT_FOUND");
        error(call("POST","/api/my/applications/9223372036854775807/payment",null,token),404,"NOT_FOUND");
        assertThat(call("PUT","/api/my/applications/"+applicationId+"/cancel",null,token).statusCode()).isEqualTo(200);
        error(call("POST",path,null,token),409,"PAYMENT_NOT_ALLOWED");
        assertThat(applications.findById(applicationId).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test void concurrentMockPaymentRecordsOneReceipt() throws Exception {
        long applicationId=id(book(1,UUID.randomUUID().toString()));
        String path="/api/my/applications/"+applicationId+"/payment";
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(() -> call("POST",path,null,token));
            var second=executor.submit(() -> call("POST",path,null,token));
            var a=first.get(15,TimeUnit.SECONDS); var b=second.get(15,TimeUnit.SECONDS);
            assertThat(a.statusCode()).isEqualTo(200); assertThat(b.statusCode()).isEqualTo(200);
            assertThat(a.body()).isEqualTo(b.body());
        }
    }

    @Test void normalBookingPersistsParticipantsAndPendingSnapshotAndIdempotentReplay() throws Exception {
        String key=UUID.randomUUID().toString(),quote=quote(2,key);long before=participants.count();
        var r=call("POST","/api/reservations",body(2,key,quote),token);long id=id(r);
        assertThat((String)JsonPath.read(r.body(),"$.application.status")).isEqualTo("PENDING");
        assertThat((Number)JsonPath.read(r.body(),"$.application.totalPrice")).isEqualTo(202);
        assertThat(participants.count()).isEqualTo(before+2);
        var replay=call("POST","/api/reservations",body(2,key,quote),token);assertThat(id(replay)).isEqualTo(id);
        assertThat(messages(id)).extracting(SmsNotification::getMessage).containsExactly("신청완료");
        error(call("POST","/api/reservations",body(1,key,quote),token),409,"IDEMPOTENCY_CONFLICT");
        assertThat(valid()).hasSize(1);assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getApplicantCount()).isEqualTo(2);
    }
    @Test void authenticationAndOwnershipAndParticipantValidation() throws Exception {
        error(call("POST","/api/reservations",body(1,"auth-test-key",null),null),401,"UNAUTHORIZED");
        error(call("POST","/api/reservations",body(1,"auth-test-key",null),staffToken),403,"FORBIDDEN");
        long id=id(book(1,"ownership-test"));
        error(call("PUT","/api/my/applications/"+id+"/cancel",null,token(member(Role.MEMBER))),404,"NOT_FOUND");
        error(call("POST","/api/reservations/quote",body(1,"mismatch-test",null).replace("\"applicantCount\":1","\"applicantCount\":2"),token),400,"INVALID_REQUEST");
        error(call("POST","/api/reservations/quote",body(1,"bad-type-test",null).replace("APPLICANT","COMPANION"),token),400,"INVALID_REQUEST");
        error(call("POST","/api/reservations/quote",body(1,"zero-count-test",null).replace("\"applicantCount\":1","\"applicantCount\":0"),token),400,"INVALID_REQUEST");
        error(call("POST","/api/reservations",body(1,"price-injection",null).replace("}","}").replaceFirst("\\}$",",\"totalPrice\":1}"),token),400,"INVALID_REQUEST");
    }
    @Test void foreignScheduleAndForeignOptionsAreRejected() throws Exception {
        var other=product("골프");var foreign=schedule(other,LocalDate.of(2026,12,4));
        String request=body(1,"foreign-schedule",null).replace("\"scheduleId\":"+schedule.getTravelScheduleId(),"\"scheduleId\":"+foreign.getTravelScheduleId());
        error(call("POST","/api/reservations/quote",request,token),400,"INVALID_REQUEST");
        selected=optionIds(other,"CLASSIC");error(call("POST","/api/reservations/quote",body(1,"foreign-options",null),token),400,"INVALID_REQUEST");
    }
    @Test void deadlineIncludesD7KoreanCalendarDayAndRejectsD6() throws Exception {
        schedule.setDepartureDate(LocalDate.of(2026,11,11));schedule.setArrivalDate(LocalDate.of(2026,11,13));schedules.saveAndFlush(schedule);
        clock.value.set(Instant.parse("2026-11-04T14:59:59Z"));token=token(customer);assertThat(quote(1,"deadline-inclusive")).isNotBlank();
        clock.value.set(Instant.parse("2026-11-04T15:00:00Z"));token=token(customer);
        error(call("POST","/api/reservations/quote",body(1,"deadline-expired",null),token),409,"BOOKING_CLOSED");
    }

    @Test void scheduleTransitionsFollowKoreanCalendarAndCatchUpAfterDowntime() throws Exception {
        var recruiting = schedule(product, LocalDate.of(2026, 11, 11));
        var confirmed = schedule(product, LocalDate.of(2026, 11, 5));
        confirmed.setStatus(ScheduleStatus.CONFIRMED_SCHEDULE);
        schedules.saveAndFlush(confirmed);
        completed(customer, confirmed);
        var cancelled = schedule(product, LocalDate.of(2026, 10, 1));
        cancelled.setStatus(ScheduleStatus.CANCELLED_SCHEDULE);
        schedules.saveAndFlush(cancelled);

        clock.value.set(Instant.parse("2026-11-04T14:59:59Z"));
        scheduleStatusScheduler.advanceDueSchedules();
        assertThat(storedStatus(recruiting)).isEqualTo(ScheduleStatus.RECRUITING_SCHEDULE);
        assertThat(storedStatus(confirmed)).isEqualTo(ScheduleStatus.CONFIRMED_SCHEDULE);

        clock.value.set(Instant.parse("2026-11-04T15:00:00Z"));
        // Simulate an older confirmed trip the scheduler never processed.
        var missedTrip = schedule(product, LocalDate.of(2026, 11, 1));
        missedTrip.setStatus(ScheduleStatus.CONFIRMED_SCHEDULE);
        schedules.saveAndFlush(missedTrip);
        completed(customer, missedTrip);
        completed(customer, missedTrip);
        scheduleStatusScheduler.advanceDueSchedules();
        assertThat(storedStatus(recruiting)).isEqualTo(ScheduleStatus.CLOSED_SCHEDULE);
        assertThat(storedStatus(confirmed)).isEqualTo(ScheduleStatus.IN_PROGRESS_SCHEDULE);
        assertThat(storedStatus(missedTrip)).isEqualTo(ScheduleStatus.COMPLETED_SCHEDULE);

        clock.value.set(Instant.parse("2026-11-08T14:59:59Z"));
        scheduleStatusScheduler.advanceDueSchedules();
        assertThat(storedStatus(confirmed)).isEqualTo(ScheduleStatus.IN_PROGRESS_SCHEDULE);

        clock.value.set(Instant.parse("2026-11-08T15:00:00Z"));
        scheduleStatusScheduler.advanceDueSchedules();
        scheduleStatusScheduler.advanceDueSchedules();
        assertThat(storedStatus(confirmed)).isEqualTo(ScheduleStatus.COMPLETED_SCHEDULE);
        assertThat(storedStatus(cancelled)).isEqualTo(ScheduleStatus.CANCELLED_SCHEDULE);
        assertThat(storedStatus(schedule)).isEqualTo(ScheduleStatus.RECRUITING_SCHEDULE);

        token = token(customer);
        var history = call("GET", "/api/my/trips/history", null, token);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(history.body(), "$.content")).hasSize(3);
        var quote = call("POST", "/api/reservations/quote", body(1, "automatic-completion", null), token);
        assertThat(quote.statusCode()).as(quote.body()).isEqualTo(200);
        assertThat((Number) JsonPath.read(quote.body(), "$.completedTrips")).isEqualTo(2);
        assertThat((Number) JsonPath.read(quote.body(), "$.price.discountRate")).isEqualTo(10);
    }

    private ScheduleStatus storedStatus(TravelSchedule schedule) {
        return schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getStatus();
    }

    @Test void honeymoonTeamsAndCapacity() throws Exception {
        product=product("허니문");schedule=schedule(product,LocalDate.of(2026,12,4));selected=optionIds(product,"GRAND");
        error(call("POST","/api/reservations/quote",body(1,"honeymoon-one",null),token),400,"INVALID_REQUEST");
        error(call("POST","/api/reservations/quote",body(4,"honeymoon-four",null),token),400,"INVALID_REQUEST");
        long first=id(book(2,"honeymoon-team1"));long second=id(book(2,"honeymoon-team2"));
        assertThat(valid()).extracting(TravelApplication::getStatus).containsOnly(ApplicationStatus.CONFIRMED);
        assertThat(messages(first)).extracting(SmsNotification::getMessage).containsExactlyInAnyOrder("신청완료", "출발확정");
        assertThat(messages(second)).extracting(SmsNotification::getMessage).containsExactly("출발확정");
        error(call("POST","/api/reservations/quote",body(2,"honeymoon-full",null),token),409,"CAPACITY_EXCEEDED");
    }
    @Test void completedTripsAreDistinctAndDiscountIgnoresMemberTravelCount() throws Exception {
        for(int trips=0;trips<=2;trips++) {
            var r=call("POST","/api/reservations/quote",body(1,"loyalty-test-"+trips,null),token);
            assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
            assertThat((Number)JsonPath.read(r.body(),"$.completedTrips")).isEqualTo(trips);
            assertThat((Number)JsonPath.read(r.body(),"$.price.discountRate")).isEqualTo(trips==2?10:0);
            if(trips==2) {assertThat((Number)JsonPath.read(r.body(),"$.price.discountAmount")).isEqualTo(10);break;}
            var past=schedule(product,LocalDate.of(2025,1,1).plusDays(trips*10));past.setStatus(ScheduleStatus.COMPLETED_SCHEDULE);schedules.saveAndFlush(past);
            completed(customer,past);completed(customer,past); // duplicate same schedule counts once
        }
    }
    @Test void quoteChangeAndTamperingRequireReconfirmationAndSnapshotRemainsStable() throws Exception {
        String key="quote-change-key",q=quote(1,key);product.setClassicPrice(999);products.saveAndFlush(product);
        error(call("POST","/api/reservations",body(1,key,q),token),409,"QUOTE_CHANGED");
        String fresh=quote(1,key);error(call("POST","/api/reservations",body(1,key,"x"+fresh),token),400,"INVALID_QUOTE");
        long id=id(call("POST","/api/reservations",body(1,key,fresh),token));product.setClassicPrice(1);products.saveAndFlush(product);
        var opt=options.findById(selected.getFirst()).orElseThrow();opt.setName("수정된 호텔");options.saveAndFlush(opt);
        var a=applications.findById(id).orElseThrow();assertThat(a.getTotalPrice()).isEqualTo(999);assertThat(a.getHotelOption()).isEqualTo("CLASSIC HOTEL");
    }
    @Test void confirmationNotifiesAllOnceAndAdditionalBookingOnlyNewApplicant() throws Exception {
        long first=id(book(2,"threshold-first"));long second=id(book(1,"threshold-second"));
        assertThat(valid()).extracting(TravelApplication::getStatus).containsOnly(ApplicationStatus.CONFIRMED);
        assertThat(messages(first)).extracting(SmsNotification::getMessage).containsExactlyInAnyOrder("신청완료", "출발확정");
        assertThat(messages(second)).extracting(SmsNotification::getMessage).containsExactly("출발확정");
        long third=id(book(1,"confirmed-additional"));
        assertThat(messages(first)).hasSize(2);
        assertThat(messages(third)).extracting(SmsNotification::getMessage).containsExactly("출발확정");
        error(call("PUT","/api/my/applications/"+first+"/cancel",null,token),409,"CANCELLATION_NOT_ALLOWED");
    }
    @Test void pendingCancellationAndClosedUnconfirmedCancellationPreserveRows() throws Exception {
        long first=id(book(1,"pending-cancel"));long travelers=participants.count();
        var r=call("PUT","/api/my/applications/"+first+"/cancel",null,token);assertThat(r.statusCode()).isEqualTo(200);
        assertThat(applications.findById(first).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(participants.count()).isEqualTo(travelers);assertThat(valid()).isEmpty();
        long second=id(book(1,"closed-cancel"));schedule=schedules.findById(schedule.getTravelScheduleId()).orElseThrow();
        schedule.setStatus(ScheduleStatus.CLOSED_SCHEDULE);schedules.saveAndFlush(schedule);
        assertThat(call("PUT","/api/my/applications/"+second+"/cancel",null,token).statusCode()).isEqualTo(200);
        assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getApplicantCount()).isZero();
    }
    @Test void staffScheduleCancelPreservesTargetsAndRejectsProgressAndCompleted() throws Exception {
        long first=id(book(2,"staff-cancel-first"));long travelers=participants.count();
        error(call("PUT","/api/admin/schedules/"+schedule.getTravelScheduleId()+"/cancel",null,token),403,"FORBIDDEN");
        var r=call("PUT","/api/admin/schedules/"+schedule.getTravelScheduleId()+"/cancel",null,staffToken);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(applications.findById(first).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(messages(first)).extracting(SmsNotification::getMessage).containsExactlyInAnyOrder("신청완료", "일정취소");
        assertThat(participants.count()).isEqualTo(travelers);
        assertThat(call("PUT","/api/admin/schedules/"+schedule.getTravelScheduleId()+"/cancel",null,staffToken).statusCode()).isEqualTo(200);
        assertThat(messages(first)).hasSize(2);
        for(var status:List.of(ScheduleStatus.IN_PROGRESS_SCHEDULE,ScheduleStatus.COMPLETED_SCHEDULE)) {
            var s=schedule(product,LocalDate.of(2026,12,4));s.setStatus(status);schedules.saveAndFlush(s);
            error(call("PUT","/api/admin/schedules/"+s.getTravelScheduleId()+"/cancel",null,staffToken),409,"INVALID_STATE");
        }
    }
    @Test void scheduleChangesAndDeletesRespectAnyApplicationHistory() throws Exception {
        long id=id(book(1,"schedule-history"));assertThat(call("PUT","/api/my/applications/"+id+"/cancel",null,token).statusCode()).isEqualTo(200);
        String path="/api/admin/schedules/"+schedule.getTravelScheduleId();
        error(call("PATCH",path,"{\"departureDate\":\"2026-12-05\",\"endDate\":\"2026-12-08\"}",staffToken),409,"HISTORY_EXISTS");
        error(call("DELETE",path,null,staffToken),409,"HISTORY_EXISTS");
        var empty=schedule(product,LocalDate.of(2026,12,10));assertThat(call("DELETE","/api/admin/schedules/"+empty.getTravelScheduleId(),null,staffToken).statusCode()).isEqualTo(204);
        error(call("POST","/api/admin/tours/"+product.getId()+"/schedules","{\"departureDate\":\"2026-12-10\",\"endDate\":\"2026-12-09\"}",staffToken),400,"INVALID_REQUEST");
    }

    @Test void lastSeatConcurrentBookingsAndThresholdConfirmationAreSerialized() throws Exception {
        id(book(8,"eight-seats-base"));String q1=quote(2,"last-seats-one"),q2=quote(2,"last-seats-two");
        var start=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)) {
            var f1=pool.submit(() -> {start.await();return call("POST","/api/reservations",body(2,"last-seats-one",q1),token);});
            var f2=pool.submit(() -> {start.await();return call("POST","/api/reservations",body(2,"last-seats-two",q2),token);});start.countDown();
            var results=List.of(f1.get(),f2.get());assertThat(results).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201,409);
        }
        assertThat(valid().stream().mapToInt(TravelApplication::getApplicantCount).sum()).isEqualTo(10);
        assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getApplicantCount()).isEqualTo(10);
    }
    @Test void simultaneousThresholdRequestsAndIdenticalReplayCreateNoDuplicateNotifications() throws Exception {
        long first=id(book(1,"threshold-base"));String q=quote(2,"threshold-replay");var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var jobs=new ArrayList<Future<HttpResponse<String>>>();
            for(int i=0;i<2;i++)jobs.add(pool.submit(() -> {gate.await();return call("POST","/api/reservations",body(2,"threshold-replay",q),token);}));
            gate.countDown();long one=id(jobs.get(0).get()),two=id(jobs.get(1).get());assertThat(one).isEqualTo(two);
        }
        assertThat(valid()).hasSize(2);
        for(var a:valid()) {
            assertThat(messages(a.getId())).extracting(SmsNotification::getNotificationType)
                .containsOnlyOnce(NotificationType.DEPARTURE_CONFIRMED)
                .hasSize(a.getId() == first ? 2 : 1);
        }
    }
    @Test void cancelVersusConfirmationAndScheduleCancelVersusBookingStayConsistent() throws Exception {
        long first=id(book(1,"race-cancel-first"));String q=quote(2,"race-confirm");var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var cancel=pool.submit(() -> {gate.await();return call("PUT","/api/my/applications/"+first+"/cancel",null,token);});
            var apply=pool.submit(() -> {gate.await();return call("POST","/api/reservations",body(2,"race-confirm",q),token);});gate.countDown();
            assertThat(cancel.get().statusCode()).isIn(200,409);assertThat(apply.get().statusCode()).isEqualTo(201);
        }
        var loaded=schedules.findById(schedule.getTravelScheduleId()).orElseThrow();assertThat(loaded.getApplicantCount()).isEqualTo(valid().stream().mapToInt(TravelApplication::getApplicantCount).sum());
        String secondQuote=quote(1,"race-staff-cancel");var gate2=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var cancel=pool.submit(() -> {gate2.await();return call("PUT","/api/admin/schedules/"+schedule.getTravelScheduleId()+"/cancel",null,staffToken);});
            var apply=pool.submit(() -> {gate2.await();return call("POST","/api/reservations",body(1,"race-staff-cancel",secondQuote),token);});gate2.countDown();
            assertThat(cancel.get().statusCode()).isEqualTo(200);assertThat(apply.get().statusCode()).isIn(201,409);
        }
        assertThat(valid()).isEmpty();assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getStatus()).isEqualTo(ScheduleStatus.CANCELLED_SCHEDULE);
    }
    @Test void deletionVersusNewBookingNeverLosesHistory() throws Exception {
        String q=quote(1,"race-delete-book");var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var del=pool.submit(() -> {gate.await();return call("DELETE","/api/admin/schedules/"+schedule.getTravelScheduleId(),null,staffToken);});
            var apply=pool.submit(() -> {gate.await();return call("POST","/api/reservations",body(1,"race-delete-book",q),token);});gate.countDown();
            int ds=del.get().statusCode(),as=apply.get().statusCode();
            assertThat(ds==204 && as==404 || ds==409 && as==201).as("delete=%s apply=%s",ds,as).isTrue();
        }
    }
    @Test void smsFailureTimeoutRetryAndRecoveryDoNotRollbackBookingOrCancellation() throws Exception {
        long id=id(book(3,"sms-failure-book"));var n=messages(id).getFirst();provider.mode="FAIL";
        sms.retryNotification(n.getId());assertThat(notifications.findById(n.getId()).orElseThrow().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
        int calls=provider.count.get();sms.retryNotification(n.getId());assertThat(provider.count.get()).isEqualTo(calls);
        clock.value.set(clock.instant().plusSeconds(86401));provider.mode="TIMEOUT";sms.retryNotification(n.getId());
        assertThat(notifications.findById(n.getId()).orElseThrow().getLastError()).isEqualTo("SMS_TIMEOUT");
        clock.value.set(clock.instant().plusSeconds(86401));provider.mode="SUCCESS";sms.retryNotification(n.getId());
        assertThat(notifications.findById(n.getId()).orElseThrow().getStatus()).isEqualTo(NotificationStatus.SENT);
        int sent=provider.count.get();sms.retryNotification(n.getId());assertThat(provider.count.get()).isEqualTo(sent);
        token=token(customer);staffToken=token(staff);
        assertThat(call("PUT","/api/admin/schedules/"+schedule.getTravelScheduleId()+"/cancel",null,staffToken).statusCode()).isEqualTo(200);
        var cancelled=messages(id).stream().filter(m -> m.getNotificationType()==NotificationType.SCHEDULE_CANCELLED).findFirst().orElseThrow();
        provider.mode="FAIL";sms.retryNotification(cancelled.getId());
        assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getStatus()).isEqualTo(ScheduleStatus.CANCELLED_SCHEDULE);
        assertThat(cancelled.getRecipientNumber()).isEqualTo("01012345678");
    }
    @Test void abandonedCommittedNotificationIsRecoveredAfterLeaseAndCancelledDepartureIsSuppressed() throws Exception {
        long id=id(book(3,"outbox-recovery"));var n=messages(id).getFirst();
        var abandoned=deliveryStore.claim(n.getId());assertThat(abandoned).isNotNull();
        // Simulate provider success followed by process loss before storing the delivery result.
        provider.sendSms(abandoned.phoneNumber(),abandoned.message(),"misterworld-sms-"+n.getId());
        int calls=provider.count.get();sms.retryNotification(n.getId());assertThat(provider.count.get()).isEqualTo(calls);
        clock.value.set(clock.instant().plusSeconds(31));sms.retryNotification(n.getId());
        assertThat(provider.count.get()).isEqualTo(calls);
        assertThat(notifications.findById(n.getId()).orElseThrow().getStatus()).isEqualTo(NotificationStatus.SENT);
        long another=id(book(1,"outbox-suppress"));assertThat(call("PUT","/api/admin/schedules/"+schedule.getTravelScheduleId()+"/cancel",null,staffToken).statusCode()).isEqualTo(200);
        var pending=messages(another).stream().filter(m -> m.getNotificationType()==NotificationType.DEPARTURE_CONFIRMED).findFirst().orElseThrow();
        calls=provider.count.get();sms.retryNotification(pending.getId());assertThat(provider.count.get()).isEqualTo(calls);
        assertThat(notifications.findById(pending.getId()).orElseThrow().getLastError()).isEqualTo("RETRY_DISABLED");
    }

    @Test void smsWithoutProviderIdempotencyRetriesOnlyConfirmedRejections() throws Exception {
        provider.safeRetry = false;
        var first = messages(id(book(3, "non-idempotent-retry"))).getFirst();
        var second = messages(id(book(1, "non-idempotent-crash"))).getFirst();

        provider.mode = "REJECTED";
        sms.retryNotification(first.getId());
        var rejected = notifications.findById(first.getId()).orElseThrow();
        assertThat(rejected.getLastError()).isEqualTo("SMS_FAILED");
        assertThat(rejected.getNextAttemptAt()).isNotNull();

        assertThat(deliveryStore.claim(second.getId())).isNotNull();
        clock.value.set(clock.instant().plusSeconds(86401));
        provider.mode = "TIMEOUT";
        int calls = provider.count.get();
        sms.retryNotification(first.getId());
        sms.retryNotification(second.getId());
        assertThat(provider.count.get()).isEqualTo(calls + 1);

        clock.value.set(clock.instant().plusSeconds(86401));
        for (var notification : List.of(first, second)) {
            var held = notifications.findById(notification.getId()).orElseThrow();
            assertThat(held.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(held.getLastError()).isEqualTo("SMS_DELIVERY_UNKNOWN");
            assertThat(deliveryStore.claim(notification.getId())).isNull();
        }
        assertThat(notifications.findDue(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            "SMS_DELIVERY_UNKNOWN", org.springframework.data.domain.Pageable.unpaged()))
            .doesNotContain(first.getId(), second.getId());
    }

    @Test void transactionFailureRollsBackApplicationParticipantsCountAndOutbox() throws Exception {
        var tx=new TransactionTemplate(transactionManager);long before=applications.count(),beforePeople=participants.count(),beforeSms=notifications.count();
        assertThatThrownBy(() -> tx.execute(status -> {
            var a=completed(customer,schedule);var p=new TravelParticipant();p.setTravelApplication(a);p.setName("롤백 여행객");p.setPhoneNumber("01012345678");
            p.setBirthDate(LocalDate.of(1990,1,1));p.setParticipantType(ParticipantType.APPLICANT);participants.saveAndFlush(p);
            var loaded=schedules.findByIdForUpdate(schedule.getTravelScheduleId()).orElseThrow();loaded.setApplicantCount(3);
            sms.createNotification(a);throw new IllegalStateException("injected storage failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(applications.count()).isEqualTo(before);assertThat(participants.count()).isEqualTo(beforePeople);
        assertThat(notifications.count()).isEqualTo(beforeSms);assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getApplicantCount()).isZero();
    }
    String productBody(boolean existing) {
        var rows=new ArrayList<String>();
        for(var o:options.findByTravelProductId(product.getId()))
            rows.add("{"+(existing?"\"id\":"+o.getId()+",":"")+"\"type\":\""+o.getType()+
                "\",\"name\":\""+o.getName()+"\",\"grade\":\""+o.getGrade()+
                "\",\"additionalPrice\":"+o.getAdditionalPrice()+",\"defaultForGrade\":"+
                (o.getDefaultForGrade()==null?"null":"\""+o.getDefaultForGrade()+"\"")+
                ",\"benefitCode\":"+(o.getBenefitCode()==null?"null":"\""+o.getBenefitCode()+"\"")+
                ",\"allowedTourGrades\":[\"CLASSIC\",\"GRAND\",\"PREMIUM\"]}");
        return "{\"name\":\"직원 상품\",\"theme\":\"골프\",\"classicPrice\":101,\"grandPrice\":201,\"premiumPrice\":301,\"options\":["+String.join(",",rows)+"]}";
    }
    @Test void staffProductAndOptionManagementValidatesPoliciesAndKeepsSnapshots() throws Exception {
        String create=productBody(false);
        error(call("POST","/api/admin/tours",create,token),403,"FORBIDDEN");
        var created=call("POST","/api/admin/tours",create,staffToken);assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        String path="/api/admin/tours/"+product.getId();long id=id(book(1,"product-snapshot"));
        var updated=call("PUT",path,productBody(true).replace("직원 상품","수정 상품").replace("\"classicPrice\":101","\"classicPrice\":555"),staffToken);
        assertThat(updated.statusCode()).as(updated.body()).isEqualTo(200);
        assertThat(applications.findById(id).orElseThrow().getTotalPrice()).isEqualTo(101);
        assertThat(applications.findById(id).orElseThrow().getHotelOption()).isEqualTo("CLASSIC HOTEL");
        assertThat(options.findById(selected.getFirst()).orElseThrow().getAllowedTourGrades()).isEqualTo("CLASSIC,GRAND,PREMIUM");
        error(call("POST","/api/admin/tours",create.replace("\"classicPrice\":101","\"classicPrice\":-1"),staffToken),400,"INVALID_REQUEST");
        error(call("POST","/api/admin/tours",create.replace("\"grandPrice\":201","\"grandPrice\":null"),staffToken),400,"INVALID_REQUEST");
        error(call("POST","/api/admin/tours",create.replace("\"name\":\"직원 상품\"","\"name\":\"\""),staffToken),400,"INVALID_REQUEST");
        error(call("POST","/api/admin/tours",create.replace("골프","허니문"),staffToken),400,"INVALID_REQUEST");
        error(call("POST","/api/admin/tours",create.replace("골프","효도").replace("\"classicPrice\":101","\"classicPrice\":null"),staffToken),400,"INVALID_REQUEST");
        error(call("POST","/api/admin/tours",create.replace("\"defaultForGrade\":\"CLASSIC\"","\"defaultForGrade\":null"),staffToken),400,"INVALID_REQUEST");
        error(call("POST","/api/admin/tours",create.replace("\"allowedTourGrades\":[\"CLASSIC\",\"GRAND\",\"PREMIUM\"]","\"allowedTourGrades\":[\"BOGUS\"]"),staffToken),400,"INVALID_REQUEST");
    }
    @Test void productAndOptionsSaveAtomicallyOnForeignOptionFailure() throws Exception {
        var foreign=product("트레킹");Long otherOption=optionIds(foreign,"CLASSIC").getFirst();
        String update=productBody(true).replace("직원 상품","저장되면 안 됨");
        String invalid="{\"id\":"+otherOption+",\"type\":\"EXTRA\",\"name\":\"다른 상품 옵션\",\"benefitCode\":\"OTHER\",\"grade\":\"CLASSIC\",\"additionalPrice\":0,\"allowedTourGrades\":[\"CLASSIC\"]}";
        update=update.substring(0,update.length()-2)+","+invalid+"]}";
        String oldName=products.findById(product.getId()).orElseThrow().getName();
        error(call("PUT","/api/admin/tours/"+product.getId(),update,staffToken),400,"INVALID_REQUEST");
        assertThat(products.findById(product.getId()).orElseThrow().getName()).isEqualTo(oldName);
        assertThat(options.findById(otherOption).orElseThrow().getAllowedTourGrades()).isNull();
    }

    @Test void participantStorageFailureRollsBackActualBookingEndpoint() throws Exception {
        String q=quote(3,"rollback-real-book");long before=applications.count(),beforePeople=participants.count(),beforeSms=notifications.count();
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("injected test failure"))
            .when(participants).save(org.mockito.ArgumentMatchers.any(TravelParticipant.class));
        try {
            error(call("POST","/api/reservations",body(3,"rollback-real-book",q),token),409,"DATA_CONFLICT");
        } finally { org.mockito.Mockito.reset(participants); }
        assertThat(applications.count()).isEqualTo(before);assertThat(participants.count()).isEqualTo(beforePeople);
        assertThat(notifications.count()).isEqualTo(beforeSms);
        assertThat(schedules.findById(schedule.getTravelScheduleId()).orElseThrow().getApplicantCount()).isZero();
    }

    @Test void changedMealChargesOnlySavedSurchargeAndDefaultsAreNotChargedTwice() throws Exception {
        for(var option:options.findByTravelProductId(product.getId())) {
            option.setAdditionalPrice("CLASSIC".equals(option.getGrade())?999:20);
            if(option.getType().equals("MEAL") && option.getGrade().equals("PREMIUM"))option.setAllowedTourGrades("CLASSIC");
            options.saveAndFlush(option);
        }
        var defaults=call("POST","/api/reservations/quote",body(2,"defaults-free",null),token);
        assertThat(defaults.statusCode()).as(defaults.body()).isEqualTo(200);
        assertThat((Number)JsonPath.read(defaults.body(),"$.price.additionalOptionPrice")).isEqualTo(0);
        var replacement=options.findByTravelProductId(product.getId()).stream()
            .filter(o -> o.getType().equals("MEAL") && o.getGrade().equals("PREMIUM")).findFirst().orElseThrow();
        selected=new ArrayList<>(options.findByTravelProductId(product.getId()).stream()
            .filter(o -> o.getDefaultForGrade().equals("CLASSIC") && !o.getType().equals("MEAL")).map(TravelOption::getId).toList());
        selected.add(replacement.getId());
        var changed=call("POST","/api/reservations/quote",body(2,"upper-meal-charge",null),token);
        assertThat(changed.statusCode()).as(changed.body()).isEqualTo(200);
        assertThat((Number)JsonPath.read(changed.body(),"$.price.basePrice")).isEqualTo(202);
        assertThat((Number)JsonPath.read(changed.body(),"$.price.additionalOptionPrice")).isEqualTo(40);
        assertThat((Number)JsonPath.read(changed.body(),"$.price.totalPrice")).isEqualTo(242);
    }
    @Test void expiredQuoteCannotBeUsedForNewApplication() throws Exception {
        String key="expired-quote-key",q=quote(1,key);
        clock.value.set(clock.instant().plusSeconds(901));token=token(customer);
        error(call("POST","/api/reservations",body(1,key,q),token),409,"QUOTE_CHANGED");
        assertThat(valid()).isEmpty();
    }

    @Test void confirmedHalfUpRoundingAppliesToDiscountAmount() throws Exception {
        product.setClassicPrice(105);products.saveAndFlush(product);
        for(int i=0;i<2;i++) {
            var past=schedule(product,LocalDate.of(2025,1,1).plusDays(i*10));past.setStatus(ScheduleStatus.COMPLETED_SCHEDULE);schedules.saveAndFlush(past);completed(customer,past);
        }
        var r=call("POST","/api/reservations/quote",body(1,"half-up-rounding",null),token);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat((Number)JsonPath.read(r.body(),"$.price.discountAmount")).isEqualTo(11);
        assertThat((Number)JsonPath.read(r.body(),"$.price.totalPrice")).isEqualTo(94);
    }
    @Test void premiumChampagneIsHiddenAndRejectedWhileClassicCanAddIt() throws Exception {
        var champagne=new TravelOption();champagne.setTravelProduct(product);champagne.setType("EXTRA");champagne.setName("기념 음료");
        champagne.setGrade("PREMIUM");champagne.setBenefitCode("CHAMPAGNE");champagne.setAllowedTourGrades("CLASSIC,PREMIUM");champagne.setAdditionalPrice(30);options.saveAndFlush(champagne);
        var classic=new ArrayList<>(selected);classic.add(champagne.getId());selected=classic;
        assertThat(call("POST","/api/reservations/quote",body(1,"classic-champagne",null),token).statusCode()).isEqualTo(200);
        var premiumIds=new ArrayList<>(optionIds(product,"PREMIUM"));premiumIds.add(champagne.getId());
        String premium=body(1,"premium-champagne",null).replace("\"grade\":\"CLASSIC\"","\"grade\":\"PREMIUM\"")
            .replace(selected.toString(),premiumIds.toString());
        error(call("POST","/api/reservations/quote",premium,token),400,"INVALID_REQUEST");
        var detail=call("GET","/api/tours/"+product.getId(),null,token);
        assertThat((List<Number>)JsonPath.read(detail.body(),"$.allowedGrades[2].allowedOptions[*].id")).extracting(Number::longValue).doesNotContain(champagne.getId());
        String create=productBody(false);
        error(call("POST","/api/admin/tours",create,staffToken),400,"INVALID_REQUEST");
    }

}

package com.misterworld.server;

import com.misterworld.server.member.entity.Member;
import com.misterworld.server.member.entity.Role;
import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.security.JwtProvider;
import com.misterworld.server.security.TokenSession;
import com.misterworld.server.security.TokenSessionRepository;
import com.misterworld.server.travelapplication.entity.ApplicationStatus;
import com.misterworld.server.travelapplication.entity.TravelApplication;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import com.misterworld.server.domain.TravelOption;
import com.misterworld.server.domain.TravelProduct;
import com.misterworld.server.repository.TravelOptionRepository;
import com.misterworld.server.repository.TravelProductRepository;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.domain.TravelSchedule;
import com.misterworld.server.repository.TravelScheduleRepository;

import com.jayway.jsonpath.JsonPath;
import com.misterworld.server.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:uc33uc34;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=FALSE")
@ActiveProfiles("local")
@org.springframework.test.context.TestPropertySource(properties="app.sms.dispatch-enabled=false")
class TravelUseCasesIntegrationTests {
    @LocalServerPort int port;
    @Autowired MemberRepository members;
    @Autowired TravelProductRepository products;
    @Autowired TravelOptionRepository options;
    @Autowired TravelScheduleRepository schedules;
    @Autowired TravelApplicationRepository applications;
    @Autowired TokenSessionRepository sessions;
    @Autowired JwtProvider jwt;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();
    private Member member;
    private TravelProduct product;
    private TravelSchedule schedule;
    private String token;
    private final Map<String, List<TravelOption>> defaults = new LinkedHashMap<>();

    @BeforeEach
    void fixtures() {
        defaults.clear();
        member = member();
        String sid = UUID.randomUUID().toString();
        sessions.saveAndFlush(new TokenSession(sid, member.getId(), "fixture", Instant.now().plusSeconds(3600)));
        token = jwt.generateAccessToken(member, sid);
        product = product("트레킹");
        schedule = schedule(product, LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(4),
            ScheduleStatus.RECRUITING_SCHEDULE);
        for (String grade : List.of("CLASSIC", "GRAND", "PREMIUM")) {
            var list = new ArrayList<TravelOption>();
            for (String type : List.of("HOTEL", "TRANSPORT", "MEAL"))
                list.add(option(product, type, grade, grade, null));
            defaults.put(grade, list);
        }
    }

    private Member member() {
        var m = new Member();
        m.setEmail(UUID.randomUUID() + "@example.com"); m.setPassword("fixture-hash"); m.setName("고객");
        m.setAddress("서울"); m.setPhoneNumber("01012345678"); m.setRole(Role.MEMBER); m.setTravelCount(7);
        return members.saveAndFlush(m);
    }

    private TravelProduct product(String theme) {
        var p = new TravelProduct(); p.setTheme(theme); p.setName("상품-" + UUID.randomUUID());
        p.setClassicPrice(Set.of("허니문", "효도").contains(theme) ? null : 100);
        p.setGrandPrice(200); p.setPremiumPrice(300);
        return products.saveAndFlush(p);
    }

    private TravelOption option(TravelProduct p, String type, String grade, String defaultGrade, String allowed) {
        var o = new TravelOption(); o.setTravelProduct(p); o.setType(type); o.setGrade(grade);
        o.setName(switch(type) {
            case "HOTEL" -> switch(grade) { case "CLASSIC" -> "3등급 호텔"; case "GRAND" -> "4등급 호텔"; default -> "5등급 호텔"; };
            case "MEAL" -> switch(grade) { case "CLASSIC" -> "도시락"; case "GRAND" -> "현지식 레스토랑"; default -> "스테이크와 샴페인"; };
            default -> "10인 고급 승합차";
        });
        o.setDefaultForGrade(defaultGrade); o.setAllowedTourGrades(allowed); o.setAdditionalPrice(20);
        return options.saveAndFlush(o);
    }

    private TravelSchedule schedule(TravelProduct p, LocalDate start, LocalDate end, ScheduleStatus status) {
        var s = new TravelSchedule(); s.setTravelProduct(p); s.setDepartureDate(start); s.setArrivalDate(end);
        s.setStatus(status); s.setApplicantCount(2); return schedules.saveAndFlush(s);
    }

    private TravelApplication application(Member m, TravelSchedule s, ApplicationStatus status) {
        var a = new TravelApplication(); a.setMember(m); a.setTravelSchedule(s); a.setStatus(status);
        a.setTourGrade("GRAND"); a.setHotelOption("신청 당시 호텔"); a.setTransportOption("신청 당시 교통");
        a.setMealOption("신청 당시 식사"); a.setExtraOption("신청 당시 추가옵션"); a.setApplicantCount(2);
        a.setBasePrice(400); a.setAdditionalOptionPrice(50); a.setDiscountRate(10); a.setDiscountAmount(45);
        a.setTotalPrice(405); a.setAppliedAt(LocalDateTime.now()); return applications.saveAndFlush(a);
    }

    private HttpResponse<String> call(String method, String path, String body, String access) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
        if (access != null) b.header("Authorization", "Bearer " + access);
        return client.send(b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() :
            HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String get(String path) throws Exception {
        var r = call("GET", path, null, token); assertThat(r.statusCode()).as(r.body()).isEqualTo(200); return r.body();
    }
    private String path() { return "/api/tours/" + product.getId(); }
    private List<Long> ids(String grade) { return defaults.get(grade).stream().map(TravelOption::getId).toList(); }
    private HttpResponse<String> validate(Long scheduleId, String grade, List<Long> ids) throws Exception {
        return call("POST", path() + "/selection/validate", "{\"scheduleId\":" + scheduleId +
            ",\"grade\":\"" + grade + "\",\"optionIds\":" + ids + "}", token);
    }
    private void error(HttpResponse<String> r, int status, String code) {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(status);
        assertThat((Number) JsonPath.read(r.body(), "$.status")).isEqualTo(status);
        assertThat((String) JsonPath.read(r.body(), "$.code")).isEqualTo(code);
    }
    private Map<String, List<Map<String, Object>>> snapshot() {
        var data = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : List.of("회원", "여행상품", "여행옵션", "여행일정", "여행신청", "물품재고", "문자알림"))
            data.put(table, jdbc.queryForList("select * from `" + table + "` order by 1"));
        return data;
    }

    @Test
    void ownCompletedConfirmedHistoryUsesStoredSelectionAndAmountInStableRecentOrder() throws Exception {
        var old = schedule(product, LocalDate.of(2025,1,1), LocalDate.of(2025,2,10), ScheduleStatus.COMPLETED_SCHEDULE);
        var recent = schedule(product, LocalDate.of(2024,12,1), LocalDate.of(2025,3,10), ScheduleStatus.COMPLETED_SCHEDULE);
        var first = application(member, old, ApplicationStatus.CONFIRMED);
        var second = application(member, recent, ApplicationStatus.CONFIRMED);
        var third = application(member, recent, ApplicationStatus.CONFIRMED);
        application(member(), recent, ApplicationStatus.CONFIRMED);
        application(member, recent, ApplicationStatus.CANCELLED);
        application(member, recent, ApplicationStatus.PENDING);
        application(member, schedule, ApplicationStatus.CONFIRMED);
        product.setName("수정된 상품명"); product.setGrandPrice(99999); products.saveAndFlush(product);
        var option = defaults.get("GRAND").getFirst(); option.setName("수정된 호텔"); options.saveAndFlush(option);
        var before = snapshot();
        String body = get("/api/my/trips/history?memberId=999999&size=2");
        assertThat((Number) JsonPath.read(body, "$.totalElements")).isEqualTo(3);
        assertThat((List<Number>) JsonPath.read(body, "$.content[*].id")).extracting(Number::longValue)
            .containsExactly(third.getId(), second.getId());
        assertThat((String) JsonPath.read(body, "$.content[0].productName")).isEqualTo("수정된 상품명");
        assertThat((String) JsonPath.read(body, "$.content[0].departureDate")).isEqualTo("2024-12-01");
        assertThat((String) JsonPath.read(body, "$.content[0].arrivalDate")).isEqualTo("2025-03-10");
        assertThat((String) JsonPath.read(body, "$.content[0].tourGrade")).isEqualTo("GRAND");
        assertThat((Number) JsonPath.read(body, "$.content[0].totalPrice")).isEqualTo(405);
        assertThat(body).contains("신청 당시 호텔", "신청 당시 식사", "신청 당시 교통").doesNotContain("password", "token", "email");
        assertThat(((Number) JsonPath.read(get("/api/my/trips/history?page=1&size=2"), "$.content[0].id")).longValue()).isEqualTo(first.getId());
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void emptyHistoryIsSuccessfulAndAllUseCaseRoutesRequireAuthentication() throws Exception {
        assertThat((List<?>) JsonPath.read(get("/api/my/trips/history"), "$.content")).isEmpty();
        for (String p : List.of("/api/my/trips/history", "/api/tours", path(), path()+"/options",
                "/api/tours/schedules", path()+"/schedules"))
            error(call("GET", p, null, null), 401, "UNAUTHORIZED");
        error(call("POST", path()+"/selection/validate", "{}", null), 401, "UNAUTHORIZED");
    }

    @Test
    void themeDetailAndProductScheduleQueriesReuseExistingContracts() throws Exception {
        var other = product("골프");
        var otherSchedule = schedule(other, LocalDate.now().plusDays(60), LocalDate.now().plusDays(63), ScheduleStatus.RECRUITING_SCHEDULE);
        var before = snapshot();
        var theme = (List<String>) JsonPath.read(get("/api/tours?theme=트레킹&size=100"), "$.content[*].theme");
        assertThat(theme).isNotEmpty().containsOnly("트레킹");
        assertThat(((Number) JsonPath.read(get(path()), "$.id")).longValue()).isEqualTo(product.getId());
        assertThat((List<Number>) JsonPath.read(get(path()+"/schedules"), "$.content[*].travelScheduleId"))
            .extracting(Number::longValue).containsExactly(schedule.getTravelScheduleId());
        assertThat((List<Number>) JsonPath.read(get("/api/tours/schedules?size=100"), "$.content[*].travelScheduleId"))
            .extracting(Number::longValue).contains(schedule.getTravelScheduleId(), otherSchedule.getTravelScheduleId());
        error(call("GET", "/api/tours/99999999", null, token), 404, "NOT_FOUND");
        error(call("GET", "/api/tours/99999999/options", null, token), 404, "NOT_FOUND");
        error(call("GET", "/api/tours/99999999/schedules", null, token), 404, "NOT_FOUND");
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void allGradesExposeSavedDefaultsIncludingTransport() throws Exception {
        String body = get(path());
        assertThat((List<String>) JsonPath.read(body, "$.allowedGrades[*].grade")).containsExactly("CLASSIC", "GRAND", "PREMIUM");
        assertThat((List<Boolean>) JsonPath.read(body, "$.allowedGrades[*].configurationReady")).containsOnly(true);
        assertThat(body).contains("3등급 호텔", "도시락", "4등급 호텔", "현지식 레스토랑", "5등급 호텔", "스테이크와 샴페인", "10인 고급 승합차");
        assertThat((List<?>) JsonPath.read(get(path()+"/options"), "$" )).hasSize(9);
    }

    @Test
    void honeymoonAndParentsNeverOfferClassicEvenWithLegacyClassicRows() throws Exception {
        for (String theme : List.of("허니문", "효도")) {
            var restricted = product(theme);
            restricted.setClassicPrice(100); products.saveAndFlush(restricted);
            option(restricted, "MEAL", "CLASSIC", "CLASSIC", "GRAND");
            var grades = (List<String>) JsonPath.read(get("/api/tours/"+restricted.getId()), "$.allowedGrades[*].grade");
            assertThat(grades).containsExactly("GRAND", "PREMIUM");
            assertThat((List<?>) JsonPath.read(get("/api/tours/"+restricted.getId()+"/options"), "$" )).isEmpty();
            error(call("POST", "/api/tours/"+restricted.getId()+"/selection/validate",
                "{\"scheduleId\":"+schedule.getTravelScheduleId()+",\"grade\":\"CLASSIC\",\"optionIds\":[]}", token), 400, "INVALID_REQUEST");
        }
    }

    @Test
    void allowedHotelAndUpperMealChangesReturnNormalizedFinalConfigurationWithoutWrites() throws Exception {
        var hotel = option(product, "HOTEL", "GRAND", null, "CLASSIC");
        var meal = option(product, "MEAL", "PREMIUM", null, "CLASSIC,GRAND");
        // Include actual rows in inventory and notifications, so no-write assertions are not vacuous.
        var completed = schedule(product, LocalDate.of(2025,1,1), LocalDate.of(2025,1,3), ScheduleStatus.COMPLETED_SCHEDULE);
        var a = application(member, completed, ApplicationStatus.CONFIRMED);
        jdbc.update("insert into `물품재고` (`여행상품_ID`,`물품명`,`재고수량`,`재고상태`,`수정일시`) values (?,?,?,?,?)",
            product.getId(), "스카프", 8, "NORMAL", LocalDateTime.now());
        jdbc.update("insert into `문자알림` (`여행신청_ID`,`알림종류`,`수신번호`,`메시지내용`,`발송상태`) values (?,?,?,?,?)",
            a.getId(), "DEPARTURE_CONFIRMED", "01012345678", "기존 알림", "PENDING");
        var before = snapshot();
        var r = validate(schedule.getTravelScheduleId(), "CLASSIC", List.of(meal.getId(), ids("CLASSIC").get(1), hotel.getId()));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(r.body(), "$.grade")).isEqualTo("CLASSIC");
        assertThat(((Number) JsonPath.read(r.body(), "$.product.id")).longValue()).isEqualTo(product.getId());
        assertThat(((Number) JsonPath.read(r.body(), "$.schedule.travelScheduleId")).longValue()).isEqualTo(schedule.getTravelScheduleId());
        assertThat((List<?>) JsonPath.read(r.body(), "$.defaultOptions")).hasSize(3);
        assertThat((List<Number>) JsonPath.read(r.body(), "$.changedOptions[*].id")).extracting(Number::longValue)
            .containsExactlyInAnyOrder(hotel.getId(), meal.getId());
        assertThat(r.body()).doesNotContain("recipientNumber", "password", "token", "email");
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void defaultConfigurationCanBeKeptAndUnknownPolicyFailsClosed() throws Exception {
        var before = snapshot();
        var r = validate(schedule.getTravelScheduleId(), "CLASSIC", ids("CLASSIC"));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(r.body(), "$.changedOptions")).isEmpty();
        var denied = option(product, "MEAL", "PREMIUM", null, null);
        var selection = new ArrayList<>(ids("CLASSIC")); selection.set(2, denied.getId());
        var afterFixture = snapshot();
        error(validate(schedule.getTravelScheduleId(), "CLASSIC", selection), 400, "INVALID_REQUEST");
        assertThat(snapshot()).isEqualTo(afterFixture);
        assertThat(before.get("여행신청")).isEqualTo(afterFixture.get("여행신청"));
    }

    @Test
    void foreignUnknownWrongTypeDuplicateAndForbiddenOptionsAreRejected() throws Exception {
        var other = product("골프");
        var foreign = option(other, "MEAL", "PREMIUM", null, "CLASSIC");
        var wrongType = option(product, "BOGUS", "PREMIUM", null, "CLASSIC");
        var forbidden = option(product, "MEAL", "PREMIUM", null, "GRAND");
        var before = snapshot();
        for (TravelOption bad : List.of(foreign, wrongType, forbidden))
            error(validate(schedule.getTravelScheduleId(), "CLASSIC", List.of(ids("CLASSIC").get(0), ids("CLASSIC").get(1), bad.getId())), 400, "INVALID_REQUEST");
        error(validate(schedule.getTravelScheduleId(), "CLASSIC", List.of(99999999L)), 404, "OPTION_NOT_FOUND");
        error(validate(schedule.getTravelScheduleId(), "CLASSIC", List.of(ids("CLASSIC").get(0), ids("CLASSIC").get(0))), 400, "INVALID_REQUEST");
        error(validate(schedule.getTravelScheduleId(), "UNKNOWN", ids("CLASSIC")), 400, "INVALID_REQUEST");
        error(validate(schedule.getTravelScheduleId(), "CLASSIC", List.of()), 400, "INVALID_REQUEST");
        error(validate(schedule.getTravelScheduleId(), "CLASSIC", List.of(ids("CLASSIC").get(0))), 400, "INVALID_REQUEST");
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void foreignMissingOrCompletedScheduleCannotBeSelected() throws Exception {
        var other = schedule(product("골프"), LocalDate.now().plusDays(30), LocalDate.now().plusDays(32), ScheduleStatus.RECRUITING_SCHEDULE);
        var completed = schedule(product, LocalDate.of(2025,1,1), LocalDate.of(2025,1,3), ScheduleStatus.COMPLETED_SCHEDULE);
        var before = snapshot();
        error(validate(other.getTravelScheduleId(), "CLASSIC", ids("CLASSIC")), 400, "INVALID_REQUEST");
        error(validate(99999999L, "CLASSIC", ids("CLASSIC")), 404, "SCHEDULE_NOT_FOUND");
        error(validate(completed.getTravelScheduleId(), "CLASSIC", ids("CLASSIC")), 400, "INVALID_REQUEST");
        assertThat((List<Boolean>) JsonPath.read(get(path()+"/schedules"), "$.content[*].selectionEnabled")).contains(true, false);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void incompleteDefaultsAreReportedWithoutInventingConfiguration() throws Exception {
        var meal = defaults.get("CLASSIC").get(2); meal.setDefaultForGrade(null); options.saveAndFlush(meal);
        assertThat((Boolean) JsonPath.read(get(path()), "$.allowedGrades[0].configurationReady")).isFalse();
        error(validate(schedule.getTravelScheduleId(), "CLASSIC", ids("CLASSIC")), 409, "CONFIGURATION_INCOMPLETE");
    }
}

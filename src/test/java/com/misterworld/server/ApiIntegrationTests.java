package com.misterworld.server;

import com.misterworld.server.member.entity.Member;
import com.misterworld.server.member.entity.Role;
import com.misterworld.server.member.repository.MemberRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.*;
import java.net.http.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@org.springframework.test.context.TestPropertySource(properties="app.sms.dispatch-enabled=false")
class ApiIntegrationTests {
    @LocalServerPort
    int port;
    @Autowired
    MemberRepository members;
    @Autowired
    PasswordEncoder encoder;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> call(String method, String path, String body, String token) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
        if (token != null)
            b.header("Authorization", "Bearer " + token);
        return client.send(b.method(method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private String token(String email) throws Exception {
        var response = call("POST", "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"test-password-123\"}", null);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private String signup(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"test-password-123\"," +
                "\"passwordConfirmation\":\"test-password-123\",\"name\":\"회원\",\"address\":\"서울\"," +
                "\"phoneNumber\":\"01012345678\"}";
    }

    @Test
    void voicePreviewRoutesRequireMembersAndValidateInput() throws Exception {
        assertThat(call("GET", "/", null, null).statusCode()).isEqualTo(302);
        assertThat(call("POST", "/api/v1/voice/interpretations", "{}", null).statusCode()).isEqualTo(401);
        assertThat(call("POST", "/api/auth/signup", signup("voice-preview@example.com"), null).statusCode()).isEqualTo(201);
        String memberToken = token("voice-preview@example.com");
        assertThat(call("POST", "/api/v1/voice/interpretations", "{}", memberToken).statusCode()).isEqualTo(400);
        var staff = new Member();
        staff.setEmail("voice-staff@example.com");
        staff.setPassword(encoder.encode("test-password-123"));
        staff.setName("Voice staff");
        staff.setAddress("Seoul");
        staff.setPhoneNumber("01012345678");
        staff.setRole(Role.STAFF);
        staff.setTravelCount(0);
        members.saveAndFlush(staff);
        assertThat(call("POST", "/api/v1/voice/interpretations", "{}", token("voice-staff@example.com")).statusCode())
            .isEqualTo(403);
    }
    @Test
    void authenticationOwnershipRefreshRotationAndRevocation() throws Exception {
        assertThat(call("GET", "/api/my/profile", null, null).statusCode()).isEqualTo(401);
        assertThat(call("POST", "/api/auth/signup", signup("first@example.com"), null).statusCode()).isEqualTo(201);
        assertThat(call("POST", "/api/auth/signup", signup("first@example.com"), null).statusCode()).isEqualTo(409);
        var member = members.findByEmail("first@example.com").orElseThrow();
        assertThat(member.getRole()).isEqualTo(Role.MEMBER);
        assertThat(member.getPassword()).doesNotContain("test-password");
        String access = token("first@example.com");
        assertThat(call("GET", "/api/unknown", null, access).statusCode()).isEqualTo(404);
        assertThat(call("DELETE", "/api/my/profile", null, access).statusCode()).isEqualTo(405);
        assertThat(call("POST", "/api/auth/signup", signup("injected@example.com").replace("}", ",\"role\":\"STAFF\"}"),
            null).statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/my/profile", null, access).body()).contains("first@example.com")
            .doesNotContain("password");
        assertThat(call("GET", "/api/admin/inventory", null, access).statusCode()).isEqualTo(403);
        assertThat(call("POST", "/api/auth/login",
            "{\"email\":\"first@example.com\",\"password\":\"wrong-password\"}", null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/api/my/applications/999", null, access).statusCode()).isEqualTo(404);
        assertThat(call("POST", "/api/reservations", "{}", access).statusCode()).isEqualTo(400);
        var login = call("POST", "/api/auth/login",
            "{\"email\":\"first@example.com\",\"password\":\"test-password-123\"}", null);
        String refresh = JsonPath.read(login.body(), "$.refreshToken");
        String oldAccess = JsonPath.read(login.body(), "$.accessToken");
        var rotated = call("POST", "/api/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", null);
        assertThat(rotated.statusCode()).as(rotated.body()).isEqualTo(200);
        assertThat(
            call("POST", "/api/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", null).statusCode()).isEqualTo(
            401);
        assertThat(call("GET", "/api/my/profile", null, oldAccess).statusCode()).isEqualTo(401);
        String newAccess = JsonPath.read(rotated.body(), "$.accessToken");
        assertThat(call("POST", "/api/auth/logout", null, newAccess).statusCode()).isEqualTo(204);
        assertThat(call("GET", "/api/my/profile", null, newAccess).statusCode()).isEqualTo(401);
    }

    @Test
    void staffInventoryBoundariesAndProductRules() throws Exception {
        var staff = new Member();
        staff.setEmail("staff@example.com");
        staff.setPassword(encoder.encode("test-password-123"));
        staff.setName("직원");
        staff.setAddress("서울");
        staff.setPhoneNumber("01012345678");
        staff.setRole(Role.STAFF);
        staff.setTravelCount(0);
        members.saveAndFlush(staff);
        String access = token("staff@example.com");
        String product = "{\"name\":\"골프 상품\",\"theme\":\"골프\",\"description\":\"설명\",\"classicPrice\":100," +
                "\"grandPrice\":200,\"premiumPrice\":300,\"options\":"+optionsJson()+"}";
        var created = call("POST", "/api/admin/tours", product, access);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        Number productId = JsonPath.read(created.body(), "$.id");
        assertThat(call("POST", "/api/admin/tours", product.replace("골프", "허니문"), access).statusCode()).isEqualTo(400);
        var inventory = call("POST", "/api/admin/inventory",
            "{\"productId\":" + productId + ",\"itemName\":\"골프공\",\"quantity\":5}", access);
        assertThat(inventory.statusCode()).as(inventory.body()).isEqualTo(201);
        Number inventoryId = JsonPath.read(inventory.body(), "$.id");
        assertThat(
            call("PATCH", "/api/admin/inventory/" + inventoryId, "{\"quantity\":-1}", access).statusCode()).isEqualTo(
            400);
        for (int value : new int[]{0, 1, 3, 4, 5}) {
            var update = call("PATCH", "/api/admin/inventory/" + inventoryId, "{\"quantity\":" + value + "}", access);
            assertThat(update.statusCode()).as(update.body()).isEqualTo(200);
            assertThat((String) JsonPath.read(update.body(), "$.status")).isEqualTo(
                value == 0 ? "OUT_OF_STOCK" : value <= 3 ? "LOW_STOCK" : "NORMAL");
        }
        assertThat(
            call("PATCH", "/api/admin/inventory/" + inventoryId, "{\"quantity\":1.5}", access).statusCode()).isEqualTo(
            400);
        assertThat(call("GET", "/api/admin/inventory?size=101", null, access).statusCode()).isEqualTo(400);
        var schedule = call("POST", "/api/admin/tours/" + productId + "/schedules",
            "{\"departureDate\":\"2027-01-01\",\"endDate\":\"2027-01-02\"}", access);
        assertThat(schedule.statusCode()).as(schedule.body()).isEqualTo(201);
        Number scheduleId = JsonPath.read(schedule.body(), "$.travelScheduleId");
        assertThat(call("DELETE", "/api/admin/schedules/" + scheduleId, null, access).statusCode()).isEqualTo(204);
    }
    private String optionsJson() {
        var rows=new java.util.ArrayList<String>();
        for(String grade:java.util.List.of("CLASSIC","GRAND","PREMIUM"))
            for(String type:java.util.List.of("HOTEL","TRANSPORT","MEAL"))
                rows.add("{\"type\":\""+type+"\",\"name\":\"기본 옵션\",\"grade\":\""+grade+
                    "\",\"additionalPrice\":0,\"defaultForGrade\":\""+grade+"\"}");
        return "["+String.join(",",rows)+"]";
    }

    @Test
    void unifiedLoginReturnsStoredRoleAndPreservesStaffAuthorization() throws Exception {
        for (Role role : Role.values()) {
            var member = new Member();
            String email = "unified-" + role.name().toLowerCase(java.util.Locale.ROOT) + "@example.com";
            member.setEmail(email);
            member.setPassword(encoder.encode("test-password-123"));
            member.setName("통합 로그인 테스트");
            member.setAddress("서울");
            member.setPhoneNumber("01000000001");
            member.setRole(role);
            member.setTravelCount(0);
            members.saveAndFlush(member);
            String body = "{\"email\":\"" + email + "\",\"password\":\"test-password-123\"}";
            var login = call("POST", "/api/auth/login", body, null);
            assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
            assertThat((String) JsonPath.read(login.body(), "$.role")).isEqualTo(role.name());
            String access = JsonPath.read(login.body(), "$.accessToken");
            assertThat(access).isNotBlank();
            assertThat((String) JsonPath.read(login.body(), "$.refreshToken")).isNotBlank();
            var admin = call("GET", "/api/admin/reservations", null, access);
            assertThat(admin.statusCode()).as(admin.body()).isEqualTo(role == Role.STAFF ? 200 : 403);
            if (role == Role.STAFF) {
                assertThat(call("POST", "/api/admin/auth/login", body, access).statusCode()).isEqualTo(404);
                assertThat(call("POST", "/api/admin/auth/signup", signup("another-staff@example.com"), access)
                    .statusCode()).isEqualTo(409);
            }
            var badPassword = call("POST", "/api/auth/login", body.replace("test-password-123", "wrong-password"), null);
            assertThat(badPassword.statusCode()).isEqualTo(401);
            assertThat((String) JsonPath.read(badPassword.body(), "$.code")).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(call("POST", "/api/auth/login", body.replace("}", ",\"role\":\"STAFF\"}"), null)
                .statusCode()).isEqualTo(400);
        }
        assertThat(call("GET", "/api/admin/reservations", null, null).statusCode()).isEqualTo(401);
        var openapi = call("GET", "/v3/api-docs", null, null);
        assertThat(openapi.statusCode()).isEqualTo(200);
        assertThat((java.util.Map<String, Object>) JsonPath.read(openapi.body(), "$.paths"))
            .containsKey("/api/auth/login").doesNotContainKey("/api/admin/auth/login");
        java.nio.file.Files.writeString(java.nio.file.Path.of("build/unified-login-openapi.json"),
            openapi.body(), java.nio.charset.StandardCharsets.UTF_8);
    }

}

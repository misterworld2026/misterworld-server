package com.misterworld.server.notification.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolapiSmsClientTests {
    private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");
    private static final String PATH = "/messages/v4/send-many/detail";
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private SolapiSmsClient client;
    private volatile String method;
    private volatile String authorization;
    private volatile String requestBody;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + PATH);
        client = new SolapiSmsClient("test-key", "test-secret", "01012345678",
            Clock.fixed(NOW, ZoneOffset.UTC), json, endpoint);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsSignedRequestAndAcceptsRegisteredMessage() throws Exception {
        respond(200, """
            {"groupInfo":{"count":{"registeredSuccess":1}},"failedMessageList":[]}
            """);
        client.sendSms("010-9876-5432", "출발 확정", "misterworld-sms-7");

        assertThat(method).isEqualTo("POST");
        var message = json.readTree(requestBody).path("messages").get(0);
        assertThat(message.path("to").asText()).isEqualTo("01098765432");
        assertThat(message.path("from").asText()).isEqualTo("01012345678");
        assertThat(message.path("text").asText()).isEqualTo("출발 확정");
        assertThat(message.path("customFields").path("deliveryKey").asText()).isEqualTo("misterworld-sms-7");
        var auth = Pattern.compile("HMAC-SHA256 apiKey=test-key, date=(.+?), salt=([a-f0-9]{32}), signature=([a-f0-9]{64})")
            .matcher(authorization);
        assertThat(auth.matches()).isTrue();
        assertThat(auth.group(1)).isEqualTo(NOW.toString());
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertThat(auth.group(3)).isEqualTo(HexFormat.of().formatHex(
            mac.doFinal((auth.group(1) + auth.group(2)).getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void classifiesMessageRejectionEvenOnHttpSuccess() {
        respond(200, """
            {"groupInfo":{"count":{"registeredSuccess":0}},"failedMessageList":[{"statusCode":"3040"}]}
            """);
        assertThatThrownBy(() -> client.sendSms("01098765432", "출발 확정"))
            .isInstanceOf(SmsClient.Rejected.class);
    }

    @Test
    void classifiesServerErrorAsUnconfirmed() {
        respond(503, "{}");
        assertThatThrownBy(() -> client.sendSms("01098765432", "출발 확정"))
            .isInstanceOf(IllegalStateException.class);
    }

    private void respond(int status, String body) {
        server.createContext(PATH, exchange -> {
            try (exchange) {
                method = exchange.getRequestMethod();
                authorization = exchange.getRequestHeaders().getFirst("Authorization");
                requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, payload.length);
                exchange.getResponseBody().write(payload);
            }
        });
        server.start();
    }
}

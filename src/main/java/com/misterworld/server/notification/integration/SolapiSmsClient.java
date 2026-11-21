package com.misterworld.server.notification.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "solapi")
public class SolapiSmsClient implements SmsClient {
    private static final Set<Integer> REJECTION_STATUS_CODES = Set.of(400, 401, 403, 404, 422, 429);

    private final String apiKey;
    private final String apiSecret;
    private final String sender;
    private final URI endpoint;
    private final Clock clock;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    @Autowired
    public SolapiSmsClient(@Value("${SOLAPI_API_KEY:}") String apiKey,
                           @Value("${SOLAPI_API_SECRET:}") String apiSecret,
                           @Value("${SOLAPI_SENDER_NUMBER:}") String sender,
                           Clock clock, ObjectMapper json) {
        this(apiKey, apiSecret, sender, clock, json, URI.create("https://api.solapi.com/messages/v4/send-many/detail"));
    }

    SolapiSmsClient(String apiKey, String apiSecret, String sender, Clock clock, ObjectMapper json, URI endpoint) {
        if (apiKey.isBlank() || apiSecret.isBlank() || !sender.matches("0[0-9]{7,10}")) {
            throw new IllegalArgumentException("Set SOLAPI_API_KEY, SOLAPI_API_SECRET and SOLAPI_SENDER_NUMBER");
        }
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.sender = sender;
        this.clock = clock;
        this.json = json;
        this.endpoint = endpoint;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        sendSms(phoneNumber, message, UUID.randomUUID().toString());
    }

    @Override
    public void sendSms(String phoneNumber, String message, String deliveryKey) {
        String recipient = phoneNumber.replace("-", "").replace(" ", "");
        if (!recipient.matches("0[0-9]{7,10}")) {
            throw new Rejected("Invalid domestic SMS recipient", "INVALID_RECIPIENT", null, null);
        }

        var body = Map.of("messages", List.of(Map.of("to", recipient, "from", sender,
            "text", message, "country", "82", "customFields", Map.of("deliveryKey", deliveryKey))));
        var request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(4))
            .header("Authorization", authorization())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
            .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new Failure("SOLAPI request interrupted", "REQUEST_INTERRUPTED", null, null);
        } catch (IOException error) {
            throw new Failure("SOLAPI response unavailable", "RESPONSE_UNAVAILABLE", null, null);
        }

        int status = response.statusCode();
        if (REJECTION_STATUS_CODES.contains(status)) {
            throw new Rejected("SOLAPI rejected request", "HTTP_REJECTED", status, readHttpErrorCode(response.body()));
        }
        if (status < 200 || status >= 300) {
            throw new Failure("Unexpected SOLAPI HTTP status", "UNEXPECTED_HTTP_STATUS",
                status, readHttpErrorCode(response.body()));
        }
        JsonNode result;
        try {
            result = json.readTree(response.body());
        } catch (JacksonException error) {
            throw new Failure("Invalid SOLAPI response", "INVALID_RESPONSE", status, null);
        }
        var registered = result.path("groupInfo").path("count").path("registeredSuccess");
        var failed = result.path("failedMessageList");
        if (registered.asInt(-1) == 0 && failed.isArray() && failed.size() == 1) {
            throw new Rejected("SOLAPI rejected message", "MESSAGE_REJECTED", status,
                safeProviderCode(failed.get(0).path("statusCode").asText()));
        }
        if (registered.asInt(-1) != 1 || !failed.isArray() || !failed.isEmpty()) {
            throw new Failure("SOLAPI did not confirm message acceptance", "ACCEPTANCE_UNKNOWN", status, null);
        }
    }

    private String readHttpErrorCode(String body) {
        try {
            return safeProviderCode(json.readTree(body).path("errorCode").asText());
        } catch (JacksonException error) {
            return null;
        }
    }

    private static String safeProviderCode(String code) {
        return code != null && code.matches("(?:[0-9]{4}|[A-Za-z][A-Za-z0-9_-]{0,63})") ? code : null;
    }

    private String authorization() {
        String date = clock.instant().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
            return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s".formatted(apiKey, date, salt, signature);
        } catch (GeneralSecurityException error) {
            throw new Rejected("Unable to sign SOLAPI request", "SIGNING_FAILED", null, null);
        }
    }
}

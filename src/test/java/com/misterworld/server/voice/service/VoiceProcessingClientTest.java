package com.misterworld.server.voice.service;

import com.misterworld.server.voice.dto.VoiceDtos.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceProcessingClientTest {
    @Test
    void sendsMultipartAndJsonToPythonContract() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> audioBody = new AtomicReference<>();
        AtomicReference<String> interpretBody = new AtomicReference<>();
        server.createContext("/internal/v1/transcribe", exchange -> {
            audioBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"text\":\"푸껫 허니문\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/internal/v1/interpret", exchange -> {
            interpretBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"fields\":{\"tour\":{\"state\":\"RESOLVED\",\"value\":\"푸껫 허니문\"},"
                    + "\"schedule\":{\"state\":\"NOT_MENTIONED\",\"value\":null},"
                    + "\"grade\":{\"state\":\"NOT_MENTIONED\",\"value\":null},"
                    + "\"hotel\":{\"state\":\"NOT_MENTIONED\",\"value\":null},"
                    + "\"transport\":{\"state\":\"NOT_MENTIONED\",\"value\":null},"
                    + "\"meal\":{\"state\":\"NOT_MENTIONED\",\"value\":null},\"extras\":[]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            VoiceProcessingClient client = new VoiceProcessingClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), 1000, 1000);
            assertEquals("푸껫 허니문", client.transcribe("audio-data".getBytes(StandardCharsets.UTF_8),
                    "sample.wav", MediaType.parseMediaType("audio/wav")));
            assertTrue(audioBody.get().contains("name=\"audio\""));
            assertTrue(audioBody.get().contains("audio-data"));

            InternalRequest request = new InternalRequest("푸껫 허니문",
                    new Selection(null, null, null, new OptionIds(null, null, null, List.of())),
                    new Candidates(List.of(), List.of(new ScheduleCandidate(10L, 1L, LocalDate.of(2027, 3, 1))), List.of()));
            assertEquals(State.RESOLVED, client.interpret(request).fields().tour().state());
            assertTrue(interpretBody.get().contains("\"candidates\""));
            assertTrue(interpretBody.get().contains("\"context\""));
            assertTrue(interpretBody.get().contains("\"departureDate\":\"2027-03-01\""));
            assertTrue(interpretBody.get().contains("\"EXTRA\":[]"));
        } finally {
            server.stop(0);
        }
    }
}

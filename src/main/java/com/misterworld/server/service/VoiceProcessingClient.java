package com.misterworld.server.service;

import com.misterworld.server.dto.voice.VoiceDtos.InternalRequest;
import com.misterworld.server.dto.voice.VoiceDtos.InternalResponse;
import com.misterworld.server.dto.voice.VoiceDtos.Transcription;
import com.misterworld.server.exception.VoiceProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

@Component
public class VoiceProcessingClient {
    private final RestClient client;

    public VoiceProcessingClient(
            @Value("${voice.python.base-url:http://127.0.0.1:8001}") String baseUrl,
            @Value("${voice.python.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${voice.python.read-timeout-ms:180000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public String transcribe(byte[] audio, String filename, MediaType contentType) {
        ByteArrayResource resource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return filename == null ? "audio" : filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(contentType);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio", new HttpEntity<>(resource, partHeaders));
        try {
            Transcription response = client.post().uri("/internal/v1/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body)
                    .retrieve().body(Transcription.class);
            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new VoiceProcessingException(HttpStatus.BAD_GATEWAY, "음성 인식 결과가 올바르지 않습니다.");
            }
            return response.text();
        } catch (RestClientException ex) {
            throw mapError(ex);
        }
    }

    public InternalResponse interpret(InternalRequest request) {
        try {
            InternalResponse response = client.post().uri("/internal/v1/interpret")
                    .contentType(MediaType.APPLICATION_JSON).body(request)
                    .retrieve().body(InternalResponse.class);
            if (response == null || response.fields() == null) {
                throw new VoiceProcessingException(HttpStatus.BAD_GATEWAY, "음성 해석 결과가 올바르지 않습니다.");
            }
            return response;
        } catch (RestClientException ex) {
            throw mapError(ex);
        }
    }

    private VoiceProcessingException mapError(RestClientException ex) {
        if (ex instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 400 || status == 413 || status == 415 || status == 422) {
                return new VoiceProcessingException(HttpStatus.valueOf(status), "음성 입력을 처리할 수 없습니다.");
            }
        }
        if (ex instanceof ResourceAccessException) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                    return new VoiceProcessingException(HttpStatus.GATEWAY_TIMEOUT, "음성 처리 시간이 초과되었습니다.");
                }
            }
        }
        return new VoiceProcessingException(HttpStatus.BAD_GATEWAY, "음성 처리 서비스에 연결할 수 없습니다.");
    }
}

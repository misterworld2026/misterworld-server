package com.misterworld.server.exception;

import org.springframework.http.HttpStatus;

public class VoiceProcessingException extends RuntimeException {
    private final HttpStatus status;

    public VoiceProcessingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

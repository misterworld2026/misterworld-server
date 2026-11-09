package com.misterworld.server.voice.dto;

import jakarta.validation.constraints.*;

import java.util.Map;

public record VoiceCommandRequest(
    @NotBlank @Size(max = 4000) String text,
    Map<String, String> context
) {
}

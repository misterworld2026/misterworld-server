package com.misterworld.server.voice.integration;

import com.misterworld.server.voice.dto.VoiceCommandRequest;


import java.util.Map;

public interface VoiceProcessingClient {
    enum InferenceState {NOT_MENTIONED, RESOLVED, FAILED}

    record VoiceInterpretation(
        Long productId,
        Long scheduleId,
        String tourGrade,
        Long hotelOptionId,
        Long transportOptionId,
        Long mealOptionId,
        Map<String, InferenceState> fields
    ) {
    }

    VoiceInterpretation interpret(VoiceCommandRequest request);
}

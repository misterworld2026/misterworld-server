package com.misterworld.server.voice.dto;

import java.util.List;

public record VoiceSelectionResult(
    Long productId,
    Long scheduleId,
    String tourGrade,
    Long hotelOptionId,
    Long transportOptionId,
    Long mealOptionId,
    int failedFieldCount,
    boolean needsConfirmation,
    List<String> unresolvedFields,
    boolean retryOrGUI
) {
}

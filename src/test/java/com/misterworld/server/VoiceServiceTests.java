package com.misterworld.server;

import com.misterworld.server.voice.integration.VoiceProcessingClient;
import com.misterworld.server.voice.integration.VoiceProcessingClient.InferenceState;
import com.misterworld.server.voice.service.VoiceService;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class VoiceServiceTests {
    @Test
    void notMentionedFieldsDoNotCountAsInferenceFailures() {
        var service = new VoiceService(null, null, null);
        assertThat(service.evaluateInferenceFailures(
            Map.of("tour", InferenceState.RESOLVED, "schedule", InferenceState.NOT_MENTIONED, "hotel",
                InferenceState.NOT_MENTIONED, "meal", InferenceState.FAILED))).isEqualTo(1);
        assertThat(service.evaluateInferenceFailures(
            Map.of("tour", InferenceState.FAILED, "hotel", InferenceState.FAILED))).isEqualTo(2);
    }

    @Test
    void incompleteSelectionAlwaysRequiresConfirmationAndCreatesNoApplication() {
        var service = new VoiceService(null, null, null);
        var result = service.matchSelection(
            new VoiceProcessingClient.VoiceInterpretation(null, null, null, null, null, null, Map.of()));
        assertThat(result.needsConfirmation()).isTrue();
        assertThat(result.unresolvedFields()).hasSize(6);
        assertThat(result.failedFieldCount()).isZero();
        assertThat(result.retryOrGUI()).isFalse();
    }
}

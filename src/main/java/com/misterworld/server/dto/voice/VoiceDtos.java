package com.misterworld.server.dto.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class VoiceDtos {
    private VoiceDtos() {}

    public record OptionIds(
            @JsonProperty("HOTEL") Long hotel,
            @JsonProperty("TRANSPORT") Long transport,
            @JsonProperty("MEAL") Long meal,
            @JsonProperty("EXTRA") List<Long> extras
    ) {
        public OptionIds {
            extras = extras == null ? List.of() : List.copyOf(extras);
        }
    }

    public record Selection(Long productId, Long scheduleId, String grade, OptionIds optionIds) {}
    public record InterpretationRequest(@NotBlank String text, @NotNull Selection context) {}
    public record Transcription(String text) {}

    public record ProductCandidate(Long id, String name, String theme, List<String> availableGrades) {}
    public record ScheduleCandidate(Long id, Long productId, LocalDate departureDate) {}
    public record OptionCandidate(Long id, Long productId, String type, String name, String grade) {}
    public record Candidates(List<ProductCandidate> products, List<ScheduleCandidate> schedules,
                             List<OptionCandidate> options) {}
    public record InternalRequest(String text, Selection context, Candidates candidates) {}

    public enum State { NOT_MENTIONED, RESOLVED, UNRESOLVED }
    public record InterpretedField(State state, String value) {}
    public record InterpretedFields(InterpretedField tour, InterpretedField schedule,
                                    InterpretedField grade, InterpretedField hotel,
                                    InterpretedField transport, InterpretedField meal,
                                    List<InterpretedField> extras) {}
    public record InternalResponse(InterpretedFields fields) {}

    public enum Status { NOT_MENTIONED, MATCHED, UNRESOLVED, INVALID, AMBIGUOUS }
    public record PreviewField(Status status, String value) {}
    public record PreviewFields(PreviewField tour, PreviewField schedule, PreviewField grade,
                                PreviewField hotel, PreviewField transport, PreviewField meal,
                                List<PreviewField> extras) {}
    public enum NextAction { REVIEW, RETRY_OR_GUI }
    public record InterpretationResponse(String text, Selection selection, PreviewFields fields,
                                         int failureCount, NextAction nextAction) {}
}

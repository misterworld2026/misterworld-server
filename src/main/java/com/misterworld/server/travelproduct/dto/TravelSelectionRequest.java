package com.misterworld.server.travelproduct.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** Additional API; all three base option types are explicitly required. */
public record TravelSelectionRequest(@NotNull @Positive Long scheduleId,
    @NotBlank String grade, @NotNull List<@NotNull @Positive Long> optionIds) {}

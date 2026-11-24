package com.misterworld.server.travelapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Changes only the grade and options of an existing application. */
public record TravelApplicationChangeRequest(
    @NotBlank String grade,
    @NotNull @Size(max = 100) List<@NotNull @Positive Long> optionIds,
    @Size(max = 1000) String quoteToken
) {}

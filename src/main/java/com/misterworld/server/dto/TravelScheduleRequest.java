package com.misterworld.server.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record TravelScheduleRequest(
    @NotNull LocalDate departureDate,
    @NotNull LocalDate endDate
) {
}

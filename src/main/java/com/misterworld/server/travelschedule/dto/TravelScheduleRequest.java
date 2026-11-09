package com.misterworld.server.travelschedule.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record TravelScheduleRequest(
    @NotNull LocalDate departureDate,
    @NotNull LocalDate endDate
) { }

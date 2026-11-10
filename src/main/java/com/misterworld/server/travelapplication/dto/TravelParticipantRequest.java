package com.misterworld.server.travelapplication.dto;

import com.misterworld.server.travelapplication.entity.ParticipantType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record TravelParticipantRequest(@NotBlank @Size(max=255) String name,
    @NotBlank @Pattern(regexp="[0-9+() -]{7,30}") String phoneNumber,
    @NotNull @PastOrPresent LocalDate birthDate, @NotNull ParticipantType participantType) {}

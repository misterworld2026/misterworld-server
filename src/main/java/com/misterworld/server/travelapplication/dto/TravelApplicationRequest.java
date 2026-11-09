package com.misterworld.server.travelapplication.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record TravelApplicationRequest(@NotNull @Positive Long productId,
    @NotNull @Positive Long scheduleId, @NotBlank String grade,
    @NotNull @Size(max=100) List<@NotNull @Positive Long> optionIds,
    @NotNull @Min(1) @Max(20) Integer applicantCount,
    @NotNull @Size(min=1,max=20) List<@NotNull @Valid TravelParticipantRequest> participants,
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String requestKey,
    @Size(max=1000) String quoteToken) {}

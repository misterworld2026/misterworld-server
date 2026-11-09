package com.misterworld.server.dto;

import jakarta.validation.constraints.*;

public record TravelOptionRequest(
    Long id,
    @NotBlank String type,
    @NotBlank @Size(max = 255) String name,
    @NotBlank String grade,
    @NotNull @Min(0) Integer additionalPrice,
    @Size(max = 2000) String description,
    String defaultForGrade
) {
}

package com.misterworld.server.travelproduct.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record TravelProductRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank String theme,
    @Size(max = 2000) String description,
    @Min(0) Integer classicPrice,
    @NotNull @Min(0) Integer grandPrice,
    @NotNull @Min(0) Integer premiumPrice,
    @NotNull @Size(max = 100) List<@NotNull @Valid TravelOptionRequest> options
) {
}

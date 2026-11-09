package com.misterworld.server.dto;

import jakarta.validation.constraints.*;

public record ItemInventoryRequest(
    @NotNull Long productId,
    @NotBlank @Size(max = 255) String itemName,
    @NotNull @Min(0) Integer quantity
) {
}

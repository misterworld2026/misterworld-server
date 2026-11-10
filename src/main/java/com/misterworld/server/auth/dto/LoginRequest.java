package com.misterworld.server.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 72) String password
) {
}

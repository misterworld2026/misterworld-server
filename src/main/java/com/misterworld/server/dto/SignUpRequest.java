package com.misterworld.server.dto;

import jakarta.validation.constraints.*;

public record SignUpRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 10, max = 72) String password,
    @NotBlank String passwordConfirmation,
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 255) String phoneNumber
) {
}

package com.misterworld.server.member.dto;

import jakarta.validation.constraints.*;

public record MemberUpdateRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 255) String phoneNumber
) {
}

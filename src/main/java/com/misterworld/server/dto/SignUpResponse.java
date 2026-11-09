package com.misterworld.server.dto;

import com.misterworld.server.domain.Role;

public record SignUpResponse(
    Long id,
    String email,
    Role role
) {
}

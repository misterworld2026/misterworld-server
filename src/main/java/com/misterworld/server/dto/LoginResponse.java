package com.misterworld.server.dto;

import com.misterworld.server.domain.Role;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Role role
) {
}

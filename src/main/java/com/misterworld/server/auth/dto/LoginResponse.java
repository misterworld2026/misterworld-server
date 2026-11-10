package com.misterworld.server.auth.dto;

import com.misterworld.server.member.entity.Role;


public record LoginResponse(
    String accessToken,
    String refreshToken,
    Role role
) {
}

package com.misterworld.server.auth.dto;

import com.misterworld.server.member.entity.Role;


public record SignUpResponse(
    Long id,
    String email,
    Role role
) {
}

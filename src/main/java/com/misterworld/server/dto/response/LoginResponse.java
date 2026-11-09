package com.misterworld.server.dto.response;

import com.misterworld.server.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private Long id;

    private String email;

    private String name;

    private Member.Role role;

    private String accessToken;

    private String refreshToken;

    private String tokenType;
}

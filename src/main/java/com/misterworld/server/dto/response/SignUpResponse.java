package com.misterworld.server.dto.response;

import com.misterworld.server.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignUpResponse {

    private long id;

    private String email;

    private String name;

    private Member.Role role;

}

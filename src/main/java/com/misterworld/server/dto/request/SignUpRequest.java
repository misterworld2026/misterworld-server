package com.misterworld.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

    @NotEmpty(message = "email은 필수 항목입니다.")
    private String email;

    @NotEmpty(message = "사용자 이름은 필수 항목입니다.")
    private String name;

    @NotEmpty(message = "주소는 필수 항목입니다.")
    private String address;

    @NotEmpty(message = "전화번호는 필수 항목입니다.")
    private String phoneNumber;

    @NotEmpty(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @NotEmpty(message = "비밀번호 확인은 필수 항목입니다.")
    private String confirmPassword;

}

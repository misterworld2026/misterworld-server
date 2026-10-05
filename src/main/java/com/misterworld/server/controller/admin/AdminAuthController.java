package com.misterworld.server.controller.admin;

import com.misterworld.server.dto.request.SignUpRequest;
import com.misterworld.server.dto.response.SignUpResponse;
import com.misterworld.server.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin-Auth", description = "관리자 전용 인증 API")
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "직원 전용 회원가입", description = "STAFF 전용 계정을 생성합니다.")
    public ResponseEntity<SignUpResponse> signup(@RequestBody @Valid SignUpRequest request) {

        SignUpResponse response = authService.createStaffAccount(request);

        return ResponseEntity.ok(response);
    }
}

package com.misterworld.server.auth.controller;

import com.misterworld.server.auth.dto.LoginRequest;
import com.misterworld.server.auth.dto.LoginResponse;
import com.misterworld.server.auth.dto.SignUpRequest;
import com.misterworld.server.auth.dto.SignUpResponse;
import com.misterworld.server.auth.service.AuthService;


import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/signup")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public SignUpResponse signup(@Valid @RequestBody SignUpRequest request) {
        return service.signup(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return service.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        service.logout(authentication.getCredentials().toString());
    }
}

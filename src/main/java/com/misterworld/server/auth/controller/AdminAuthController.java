package com.misterworld.server.auth.controller;

import com.misterworld.server.auth.dto.SignUpRequest;
import com.misterworld.server.auth.dto.SignUpResponse;
import com.misterworld.server.auth.service.AuthService;


import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService service;

    @PostMapping("/signup")
    public SignUpResponse signup(@Valid @RequestBody SignUpRequest request) {
        return service.createStaffAccount(request);
    }

}

package com.misterworld.server.member.controller;

import com.misterworld.server.member.dto.MemberResponse;
import com.misterworld.server.member.dto.MemberUpdateRequest;
import com.misterworld.server.member.service.MemberService;


import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/my/profile")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService service;

    @GetMapping
    public MemberResponse getMyInfo(Authentication authentication) {
        return MemberResponse.from(service.getMyInfo(Long.valueOf(authentication.getName())));
    }

    @PutMapping
    public MemberResponse updateMyInfo(Authentication authentication, @Valid @RequestBody MemberUpdateRequest request) {
        return MemberResponse.from(service.updateMyInfo(Long.valueOf(authentication.getName()), request));
    }
}

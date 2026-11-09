package com.misterworld.server.travelapplication.controller;

import com.misterworld.server.travelapplication.dto.ApplicationResponse;
import com.misterworld.server.travelapplication.service.MockPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MockPaymentController {
    private final MockPaymentService service;

    @PostMapping("/api/my/applications/{applicationId}/payment")
    public ApplicationResponse completePayment(@PathVariable Long applicationId, Authentication authentication) {
        return service.completePayment(applicationId, Long.valueOf(authentication.getName()));
    }
}

package com.misterworld.server.controller;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.dto.ApplicationResponse;
import com.misterworld.server.service.TravelApplicationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
public class TravelApplicationController {
    private final TravelApplicationService service;

    @PostMapping("/api/reservations")
    public void apply(Authentication authentication) {
        service.apply(Long.valueOf(authentication.getName()));
    }

    @GetMapping("/api/my/applications")
    public Page<ApplicationResponse> getMyApplication(Authentication authentication,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.getMyApplication(Long.valueOf(authentication.getName()), Pages.of(page, size, "id"));
    }

    @GetMapping("/api/my/applications/{applicationId}")
    public ApplicationResponse getApplication(@PathVariable Long applicationId, Authentication authentication) {
        return service.getApplication(applicationId, Long.valueOf(authentication.getName()));
    }

    @GetMapping("/api/my/trips/history")
    public Page<ApplicationResponse> getTravelHistory(Authentication authentication,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.getTravelHistory(Long.valueOf(authentication.getName()), Pages.of(page, size, "id"));
    }

    @PutMapping("/api/my/applications/{applicationId}/cancel")
    public void cancelApplication(@PathVariable Long applicationId, Authentication authentication) {
        service.cancelApplication(applicationId, Long.valueOf(authentication.getName()));
    }

    @GetMapping("/api/admin/reservations")
    public Page<ApplicationResponse> getApplications(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.getApplications(Pages.of(page, size, "id"));
    }
}

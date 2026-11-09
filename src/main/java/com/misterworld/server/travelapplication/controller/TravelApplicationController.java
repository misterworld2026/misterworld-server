package com.misterworld.server.travelapplication.controller;

import com.misterworld.server.common.Pages;
import com.misterworld.server.travelapplication.dto.ApplicationResponse;
import com.misterworld.server.travelapplication.dto.TravelApplicationRequest;
import com.misterworld.server.travelapplication.dto.TravelApplicationResponse;
import com.misterworld.server.travelapplication.dto.TravelHistoryResponse;
import com.misterworld.server.travelapplication.dto.TravelQuoteResponse;
import com.misterworld.server.travelapplication.service.TravelApplicationService;


import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
public class TravelApplicationController {
    private final TravelApplicationService service;

    @PostMapping("/api/reservations")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public TravelApplicationResponse apply(Authentication authentication,
            @jakarta.validation.Valid @RequestBody TravelApplicationRequest request) {
        return service.apply(Long.valueOf(authentication.getName()),request);
    }

    @PostMapping("/api/reservations/quote")
    public TravelQuoteResponse quote(Authentication authentication,
            @jakarta.validation.Valid @RequestBody TravelApplicationRequest request) {
        return service.quote(Long.valueOf(authentication.getName()),request);
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
    public Page<TravelHistoryResponse> getTravelHistory(Authentication authentication,
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

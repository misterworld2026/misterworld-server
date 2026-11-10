package com.misterworld.server.notification.controller;

import com.misterworld.server.common.Pages;
import com.misterworld.server.notification.dto.NotificationResponse;
import com.misterworld.server.notification.service.SmsNotificationService;


import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final SmsNotificationService service;

    @GetMapping("/api/my/notifications")
    public Page<NotificationResponse> getMyNotifications(Authentication authentication, @RequestParam(defaultValue =
        "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.getNotifications(Long.valueOf(authentication.getName()), Pages.of(page, size, "id"));
    }

    @GetMapping("/api/admin/notifications")
    public Page<NotificationResponse> getNotifications(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return service.getNotifications(null, Pages.of(page, size, "id"));
    }
}

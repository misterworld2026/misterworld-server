package com.misterworld.server.service;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.dto.NotificationResponse;
import com.misterworld.server.repository.SmsNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SmsNotificationService {
    private final SmsNotificationRepository notifications;

    public Page<NotificationResponse> getNotifications(Long memberId, Pageable pageable) {
        return (memberId == null ? notifications.findAll(pageable) : notifications.findByMemberId(memberId,
            pageable)).map(NotificationResponse::from);
    }
}

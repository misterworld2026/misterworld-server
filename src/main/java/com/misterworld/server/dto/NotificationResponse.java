package com.misterworld.server.dto;

import com.misterworld.server.domain.*;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    Long applicationId,
    NotificationType notificationType,
    String message,
    NotificationStatus status,
    LocalDateTime sentAt
) {
    public static NotificationResponse from(SmsNotification n) {
        return new NotificationResponse(n.getId(), n.getTravelApplication().getId(), n.getNotificationType(),
            n.getMessage(), n.getStatus(), n.getSentAt());
    }
}

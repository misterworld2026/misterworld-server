package com.misterworld.server.notification.dto;

import com.misterworld.server.notification.entity.NotificationStatus;
import com.misterworld.server.notification.entity.NotificationType;
import com.misterworld.server.notification.entity.SmsNotification;


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

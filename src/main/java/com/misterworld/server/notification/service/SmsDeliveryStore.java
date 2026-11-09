package com.misterworld.server.notification.service;

import com.misterworld.server.notification.integration.SmsClient;
import com.misterworld.server.notification.repository.SmsNotificationRepository;
import com.misterworld.server.travelapplication.entity.ApplicationStatus;
import com.misterworld.server.travelschedule.entity.ScheduleStatus;

import com.misterworld.server.notification.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

/** Additional short transactions for durable outbox claims/results; no network calls here. */
@Service @RequiredArgsConstructor
public class SmsDeliveryStore {
    private static final String DELIVERY_IN_FLIGHT = "SMS_IN_FLIGHT";
    static final String DELIVERY_UNKNOWN = "SMS_DELIVERY_UNKNOWN";

    private final SmsNotificationRepository notifications;
    private final SmsClient client;
    private final Clock clock;
    private final com.misterworld.server.travelapplication.service.BookingPolicy policy;
    public record Delivery(Long notificationId,String phoneNumber,String message,String claimToken) {}
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
    private boolean retryEnabled(SmsNotification n) {
        var a=n.getTravelApplication(); var schedule=a.getTravelSchedule();
        if (n.getNotificationType() == NotificationType.SCHEDULE_CANCELLED) return true;
        if (schedule.getStatus() == ScheduleStatus.CANCELLED_SCHEDULE
            || !policy.today(clock).isBefore(schedule.getDepartureDate())) return false;
        return n.getNotificationType() == NotificationType.APPLICATION_COMPLETED
            ? a.getStatus() != ApplicationStatus.CANCELLED
            : a.getStatus() == ApplicationStatus.CONFIRMED;
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public Delivery claim(Long notificationId) {
        var n=notifications.findByIdForUpdate(notificationId).orElse(null);
        var now=now();
        if(n==null || n.getStatus()==NotificationStatus.SENT || DELIVERY_UNKNOWN.equals(n.getLastError()) ||
            (n.getLockedUntil()!=null && n.getLockedUntil().isAfter(now)) ||
            (n.getNextAttemptAt()!=null && n.getNextAttemptAt().isAfter(now))) return null;
        if (DELIVERY_IN_FLIGHT.equals(n.getLastError())) {
            // The previous worker may have sent the request before losing its lease.
            finishAttempt(n, NotificationStatus.FAILED, DELIVERY_UNKNOWN);
            return null;
        }
        if(!retryEnabled(n)) {
            n.setStatus(NotificationStatus.FAILED); n.setLastError("RETRY_DISABLED");
            n.setNextAttemptAt(now.plusDays(1)); n.setLockedUntil(null); return null;
        }
        n.setStatus(NotificationStatus.PENDING); n.setClaimToken(UUID.randomUUID().toString());
        n.setLockedUntil(now.plusSeconds(30)); n.setAttemptCount(n.getAttemptCount()+1);
        if (!client.supportsSafeRetry()) n.setLastError(DELIVERY_IN_FLIGHT);
        return new Delivery(n.getId(),n.getRecipientNumber(),n.getMessage(),n.getClaimToken());
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void recordDeliveryResult(Long notificationId,String claimToken,NotificationStatus status,String error) {
        var n=notifications.findByIdForUpdate(notificationId).orElseThrow();
        if(n.getStatus()==NotificationStatus.SENT || !Objects.equals(n.getClaimToken(),claimToken)) return;
        finishAttempt(n, status, error);
    }

    private void finishAttempt(SmsNotification n, NotificationStatus status, String error) {
        n.setStatus(status); n.setLastError(error); n.setLockedUntil(null); n.setClaimToken(null);
        if(status==NotificationStatus.SENT) { n.setSentAt(now()); n.setNextAttemptAt(null); }
        else if (DELIVERY_UNKNOWN.equals(error)) n.setNextAttemptAt(null);
        else n.setNextAttemptAt(now().plusDays(1));
    }
}

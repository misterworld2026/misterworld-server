package com.misterworld.server.notification.service;

import com.misterworld.server.notification.dto.NotificationResponse;
import com.misterworld.server.notification.integration.SmsClient;
import com.misterworld.server.notification.repository.SmsNotificationRepository;
import com.misterworld.server.travelapplication.entity.TravelApplication;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;

import com.misterworld.server.notification.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.concurrent.*;

@Service @RequiredArgsConstructor
public class SmsNotificationService {
    private final SmsNotificationRepository notifications;
    private final TravelApplicationRepository applications;
    private final SmsDeliveryStore deliveryStore;
    private final SmsClient client;
    private final Clock clock;
    private final org.springframework.core.env.Environment environment;
    private final ExecutorService sender=Executors.newFixedThreadPool(4);

    @Transactional(readOnly=true)
    public Page<NotificationResponse> getNotifications(Long memberId, Pageable pageable) {
        return (memberId==null ? notifications.findAll(pageable) : notifications.findByMemberId(memberId,pageable))
            .map(NotificationResponse::from);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void sendDepartureNotifications(Long scheduleId) {
        applications.findNotificationTargets(scheduleId).forEach(this::createNotification);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void sendDepartureNotification(Long scheduleId) { sendDepartureNotifications(scheduleId); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void handleDepartureConfirmed(Long scheduleId) { sendDepartureNotifications(scheduleId); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void sendConfirmationNotification(TravelApplication application) { createNotification(application); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void createNotification(TravelApplication application) {
        createNotification(application,NotificationType.DEPARTURE_CONFIRMED);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void createNotification(TravelApplication application, NotificationType notificationType) {
        if(notifications.existsByTravelApplicationIdAndNotificationType(application.getId(),notificationType)) return;
        var n=new SmsNotification(); n.setTravelApplication(application); n.setNotificationType(notificationType);
        n.setRecipientNumber(application.getMember().getPhoneNumber());
        n.setMessage(switch (notificationType) {
            case APPLICATION_COMPLETED -> "신청완료";
            case DEPARTURE_CONFIRMED -> "출발확정";
            case SCHEDULE_CANCELLED -> "일정취소";
        });
        n.setStatus(NotificationStatus.PENDING); n.setCreatedAt(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));
        notifications.save(n);
    }
    // Polling reads committed outbox records, including records left by a stopped server.
    @Scheduled(fixedDelayString="${app.sms.poll-ms:1000}")
    public void retryFailedNotifications() {
        if(!environment.getProperty("app.sms.dispatch-enabled",Boolean.class,true)) return;
        dispatchPending();
    }
    public void dispatchPending() {
        var now=LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);
        for(Long notificationId:notifications.findDue(now,SmsDeliveryStore.DELIVERY_UNKNOWN,PageRequest.of(0,50)))
            retryNotification(notificationId);
    }
    public void retryNotification(Long notificationId) {
        var delivery=deliveryStore.claim(notificationId);
        if(delivery==null) return;
        Future<?> task=sender.submit(() -> client.sendSms(delivery.phoneNumber(),delivery.message(),
            "misterworld-sms-"+delivery.notificationId()));
        try {
            task.get(environment.getProperty("app.sms.timeout-ms",Long.class,5000L),TimeUnit.MILLISECONDS);
        } catch(ExecutionException | TimeoutException | InterruptedException error) {
            task.cancel(true);
            boolean rejected = error instanceof ExecutionException && error.getCause() instanceof SmsClient.Rejected;
            String failureCode = error instanceof TimeoutException ? "SMS_TIMEOUT" : "SMS_FAILED";
            if (!client.supportsSafeRetry() && !rejected) failureCode = SmsDeliveryStore.DELIVERY_UNKNOWN;
            try {
                recordDeliveryResult(notificationId,delivery.claimToken(),NotificationStatus.FAILED,failureCode);
            } finally {
                if(error instanceof InterruptedException) Thread.currentThread().interrupt();
            }
            return;
        }
        recordDeliveryResult(notificationId,delivery.claimToken(),NotificationStatus.SENT,null);
    }
    public void recordDeliveryResult(Long notificationId,String claimToken,NotificationStatus status,String error) {
        updateStatus(notificationId,claimToken,status,error);
    }
    public void upDateStatus(Long notificationId,String claimToken,NotificationStatus status,String error) {
        updateStatus(notificationId,claimToken,status,error);
    }
    public void updateStatus(Long notificationId,String claimToken,NotificationStatus status,String error) {
        deliveryStore.recordDeliveryResult(notificationId,claimToken,status,error);
    }
    @PreDestroy void shutdown() { sender.shutdownNow(); }
}

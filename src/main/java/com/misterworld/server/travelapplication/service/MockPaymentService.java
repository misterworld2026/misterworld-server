package com.misterworld.server.travelapplication.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.travelapplication.dto.ApplicationResponse;
import com.misterworld.server.travelapplication.entity.*;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import com.misterworld.server.repository.TravelScheduleRepository;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.inventory.service.ItemInventoryService;
import com.misterworld.server.notification.service.SmsNotificationService;
import com.misterworld.server.notification.entity.NotificationType;
import com.misterworld.server.travelschedule.service.TravelScheduleService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

/** Mock completion only: no PG request, charge or refund occurs. */
@Service
@RequiredArgsConstructor
public class MockPaymentService {
    private final ApplicationScheduleLookup lookup;
    private final TravelScheduleRepository schedules;
    private final TravelApplicationRepository applications;
    private final EntityManager entityManager;
    private final Clock clock;
    private final TravelScheduleService scheduleService;
    private final ItemInventoryService inventory;
    private final SmsNotificationService notifications;

    @Transactional
    public ApplicationResponse completePayment(Long applicationId, Long memberId) {
        Long scheduleId = lookup.scheduleId(applicationId, memberId);
        var schedule = schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        var application = applications.findByIdAndMemberId(applicationId, memberId)
            .orElseThrow(BusinessException::notFound);
        entityManager.refresh(application);
        // A replay returns the original receipt, including after subsequent cancellation.
        if (application.getPaymentStatus() == PaymentStatus.PAID) return ApplicationResponse.from(application);
        if (application.getStatus() == ApplicationStatus.CANCELLED
                || schedule.getStatus() == ScheduleStatus.CANCELLED_SCHEDULE) {
            throw new BusinessException(409, "PAYMENT_NOT_ALLOWED", "취소된 신청은 모의 결제할 수 없습니다.");
        }
        scheduleService.validateAvailability(schedule, application.getApplicantCount());
        inventory.deductForPaidApplication(applicationId, scheduleId,
            schedule.getTravelProduct().getId(), application.getApplicantCount());
        application.setPaymentStatus(PaymentStatus.PAID);
        application.setPaidAmount(application.getTotalPrice());
        application.setPaidAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        scheduleService.updateApplicantCount(scheduleId);
        notifications.createNotification(application, NotificationType.APPLICATION_COMPLETED);
        if (schedule.getStatus() == ScheduleStatus.CONFIRMED_SCHEDULE) {
            application.setStatus(ApplicationStatus.CONFIRMED);
            notifications.sendConfirmationNotification(application);
        } else {
            scheduleService.checkDepartureCondition(scheduleId);
        }
        return ApplicationResponse.from(application);
    }
}

package com.misterworld.server.travelapplication.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.travelapplication.dto.ApplicationResponse;
import com.misterworld.server.travelapplication.entity.*;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import com.misterworld.server.repository.TravelScheduleRepository;
import com.misterworld.server.domain.ScheduleStatus;
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
        application.setPaymentStatus(PaymentStatus.PAID);
        application.setPaidAmount(application.getTotalPrice());
        application.setPaidAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return ApplicationResponse.from(application);
    }
}

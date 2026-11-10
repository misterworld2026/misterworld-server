package com.misterworld.server.travelapplication.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Resolves ownership in a separate read transaction before taking the schedule lock. */
@Service @RequiredArgsConstructor
public class ApplicationScheduleLookup {
    private final TravelApplicationRepository applications;
    @Transactional(propagation=Propagation.REQUIRES_NEW,readOnly=true)
    public Long scheduleId(Long applicationId,Long memberId) {
        return applications.findByIdAndMemberId(applicationId,memberId).orElseThrow(BusinessException::notFound)
            .getTravelSchedule().getTravelScheduleId();
    }
}

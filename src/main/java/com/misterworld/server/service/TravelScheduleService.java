package com.misterworld.server.service;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.domain.*;
import com.misterworld.server.dto.*;
import com.misterworld.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

import java.time.LocalDate;

import static com.misterworld.server.common.BusinessException.require;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelScheduleService {
    private final TravelScheduleRepository schedules;
    private final TravelProductRepository products;
    private final TravelApplicationRepository applications;

    public Page<TravelScheduleResponse> getSchedules(Long productId, Pageable pageable) {
        if (productId != null && !products.existsById(productId))
            throw BusinessException.notFound();
        return buildScheduleSelectionResponse(
            productId == null ? schedules.findAll(pageable) : schedules.findByTravelProductId(productId, pageable));
    }

    public Page<TravelScheduleResponse> buildScheduleSelectionResponse(Page<TravelSchedule> schedules) {
        return schedules.map(TravelScheduleResponse::from);
    }

    @Transactional
    public TravelScheduleResponse saveSchedule(Long productId, TravelScheduleRequest request) {
        validateDates(request.departureDate(), request.endDate());
        var product = products.findById(productId).orElseThrow(BusinessException::notFound);
        var schedule = new TravelSchedule();
        schedule.setTravelProduct(product);
        schedule.setDepartureDate(request.departureDate());
        schedule.setArrivalDate(request.endDate());
        schedule.setApplicantCount(0);
        schedule.setStatus(ScheduleStatus.RECRUITING_SCHEDULE);
        return TravelScheduleResponse.from(schedules.save(schedule));
    }

    @Transactional
    public TravelScheduleResponse updateSchedule(Long scheduleId, TravelScheduleRequest request) {
        var schedule = schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        validateDates(request.departureDate(), request.endDate());
        validateScheduleChange(schedule, request, applications.existsByScheduleId(scheduleId));
        schedule.setDepartureDate(request.departureDate());
        schedule.setArrivalDate(request.endDate());
        return TravelScheduleResponse.from(schedule);
    }

    public void validateDates(LocalDate departureDate, LocalDate endDate) {
        require(departureDate != null && endDate != null && !endDate.isBefore(departureDate), "종료일은 출발일보다 빠를 수 없습니다.");
    }

    public void validateScheduleChange(TravelSchedule schedule, TravelScheduleRequest request,
                                       boolean hasApplications) {
        require(schedule.getStatus() == ScheduleStatus.RECRUITING_SCHEDULE, "모집 중 일정만 날짜를 수정할 수 있습니다.");
        if (hasApplications && (!schedule.getDepartureDate()
            .equals(request.departureDate()) || !schedule.getArrivalDate().equals(request.endDate())))
            throw BusinessException.policy();
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        var schedule = schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        if (applications.existsByScheduleId(scheduleId))
            throw new BusinessException(409, "HISTORY_EXISTS", "신청 기록이 있는 일정은 삭제할 수 없습니다.");
        if (schedule.getStatus() == ScheduleStatus.IN_PROGRESS_SCHEDULE || schedule.getStatus() == ScheduleStatus.COMPLETED_SCHEDULE)
            throw new BusinessException(409, "INVALID_STATE", "진행·완료 일정은 삭제할 수 없습니다.");
        schedules.delete(schedule);
    }
}

package com.misterworld.server.travelschedule.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.notification.entity.NotificationType;
import com.misterworld.server.notification.service.SmsNotificationService;
import com.misterworld.server.travelapplication.entity.ApplicationStatus;
import com.misterworld.server.travelapplication.entity.TravelApplication;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import com.misterworld.server.travelapplication.service.BookingPolicy;
import com.misterworld.server.repository.TravelProductRepository;
import com.misterworld.server.travelschedule.dto.ScheduleResponse;
import com.misterworld.server.travelschedule.dto.TravelScheduleRequest;
import com.misterworld.server.travelschedule.dto.TravelScheduleResponse;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.domain.TravelSchedule;
import com.misterworld.server.repository.TravelScheduleRepository;


import lombok.RequiredArgsConstructor;

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
    private final SmsNotificationService notifications;
    private final BookingPolicy policy;
    private final java.time.Clock clock;

    public Page<TravelScheduleResponse> getSchedules(Long productId, Pageable pageable) {
        if (productId != null && !products.existsById(productId))
            throw BusinessException.notFound();
        return buildScheduleSelectionResponse(
            productId == null ? schedules.findAll(pageable) : schedules.findByTravelProductId(productId, pageable));
    }

    public Page<TravelScheduleResponse> buildScheduleSelectionResponse(Page<TravelSchedule> schedules) {
        return schedules.map(this::getScheduleResponse);
    }

    public TravelScheduleResponse validateSelection(Long productId, Long scheduleId) {
        var schedule = schedules.findById(scheduleId).orElseThrow(() ->
            new BusinessException(404, "SCHEDULE_NOT_FOUND", "일정을 찾을 수 없습니다."));
        require(schedule.getTravelProduct().getId().equals(productId), "다른 상품의 일정입니다.");
        require(schedule.getStatus() == ScheduleStatus.RECRUITING_SCHEDULE ||
            schedule.getStatus() == ScheduleStatus.CONFIRMED_SCHEDULE, "선택할 수 없는 일정 상태입니다.");
        return getScheduleResponse(schedule);
    }

    @Transactional
    public ScheduleResponse saveSchedule(Long productId, TravelScheduleRequest request) {
        validateDates(request.departureDate(), request.endDate());
        var product = products.findById(productId).orElseThrow(BusinessException::notFound);
        var schedule = createSchedule(request);
        schedule.setTravelProduct(product);
        schedule.setDepartureDate(request.departureDate());
        schedule.setArrivalDate(request.endDate());
        schedule.setApplicantCount(0);
        schedule.setStatus(ScheduleStatus.RECRUITING_SCHEDULE);
        return ScheduleResponse.from(getScheduleResponse(schedules.save(schedule)));
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, TravelScheduleRequest request) {
        var schedule = schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        validateDates(request.departureDate(), request.endDate());
        validateScheduleChange(schedule, request, applications.existsByScheduleId(scheduleId));
        schedule.setDepartureDate(request.departureDate());
        schedule.setArrivalDate(request.endDate());
        return ScheduleResponse.from(getScheduleResponse(schedule));
    }

    public void validateDates(LocalDate departureDate, LocalDate endDate) {
        require(departureDate != null && endDate != null && !endDate.isBefore(departureDate), "종료일은 출발일보다 빠를 수 없습니다.");
    }

    public void validateScheduleChange(TravelSchedule schedule, TravelScheduleRequest request,
                                       boolean hasApplications) {
        require(schedule.getStatus() == ScheduleStatus.RECRUITING_SCHEDULE, "모집 중 일정만 날짜를 수정할 수 있습니다.");
        if (hasApplications && (!schedule.getDepartureDate()
            .equals(request.departureDate()) || !schedule.getArrivalDate().equals(request.endDate())))
            throw new BusinessException(409, "HISTORY_EXISTS", "신청 기록이 있는 일정의 날짜는 변경할 수 없습니다.");
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
    public TravelSchedule createSchedule(TravelScheduleRequest request) {
        validateDates(request.departureDate(),request.endDate());
        var schedule=new TravelSchedule(); schedule.setDepartureDate(request.departureDate());
        schedule.setArrivalDate(request.endDate()); schedule.setApplicantCount(0);
        schedule.setStatus(ScheduleStatus.RECRUITING_SCHEDULE); return schedule;
    }

    @Transactional
    public TravelSchedule validateAndLockSchedule(Long scheduleId, Integer applicantCount) {
        var schedule=schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        validateAvailability(schedule,applicantCount);
        return schedule;
    }

    public void validateAvailability(TravelSchedule schedule,int applicantCount) {
        require(applicantCount>0,"신청 인원은 양수여야 합니다.");
        if(schedule.getStatus()!=ScheduleStatus.RECRUITING_SCHEDULE && schedule.getStatus()!=ScheduleStatus.CONFIRMED_SCHEDULE)
            throw new BusinessException(409,"SCHEDULE_UNAVAILABLE","신청할 수 없는 일정 상태입니다.");
        if(!policy.withinDeadline(schedule.getDepartureDate(),clock))
            throw new BusinessException(409,"BOOKING_CLOSED","신청 기한이 지났습니다.");
        String theme=schedule.getTravelProduct().getTheme();
        if("허니문".equals(theme)) require(policy.validHoneymoonSize(applicantCount),"허니문 신청 단위에 맞지 않습니다.");
        int capacity=switch(theme) { case "허니문" -> 4; case "효도", "트레킹" -> 20; case "골프" -> 10;
            default -> throw new BusinessException(409,"CONFIGURATION_INCOMPLETE","테마 정원이 정의되지 않았습니다."); };
        int current=calculateValidApplicantCount(applications.findValidApplicationsByScheduleId(schedule.getTravelScheduleId()));
        if(current+applicantCount>capacity) throw new BusinessException(409,"CAPACITY_EXCEEDED","일정 정원을 초과합니다.");
    }

    @Transactional
    public void updateApplicantCount(Long scheduleId) {
        var schedule=schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        schedule.setApplicantCount(calculateValidApplicantCount(applications.findValidApplicationsByScheduleId(scheduleId)));
    }
    public int calculateValidApplicantCount(java.util.List<TravelApplication> applications) {
        return applications.stream().mapToInt(a -> a.getStatus()==ApplicationStatus.CANCELLED ? 0 : a.getApplicantCount()).sum();
    }
    public boolean isDepartureConditionMet(TravelSchedule schedule,
            java.util.List<TravelApplication> applications) {
        int total=calculateValidApplicantCount(applications);
        return "허니문".equals(schedule.getTravelProduct().getTheme()) ? total>=4 &&
            applications.stream().filter(a -> a.getStatus()!=ApplicationStatus.CANCELLED)
                .allMatch(a -> a.getApplicantCount()%2==0) : total>=3;
    }
    @Transactional
    public boolean checkDepartureCondition(Long scheduleId) {
        var schedule=schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        if(schedule.getStatus()!=ScheduleStatus.RECRUITING_SCHEDULE ||
            !isDepartureConditionMet(schedule,applications.findValidApplicationsByScheduleId(scheduleId))) return false;
        confirmDeparture(schedule); confirmPendingApplications(scheduleId);
        notifications.sendDepartureNotifications(scheduleId); return true;
    }
    public void confirmDeparture(TravelSchedule schedule) {
        require(schedule.getStatus()==ScheduleStatus.RECRUITING_SCHEDULE,"모집 중 일정만 출발 확정할 수 있습니다.");
        schedule.setStatus(ScheduleStatus.CONFIRMED_SCHEDULE);
    }
    public void confirmPendingApplications(Long scheduleId) { applications.confirmPendingApplications(scheduleId); }

    @Transactional
    public TravelScheduleResponse cancelSchedule(Long scheduleId) {
        var schedule=schedules.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        if(schedule.getStatus()==ScheduleStatus.CANCELLED_SCHEDULE) return getScheduleResponse(schedule);
        if(!java.util.Set.of(ScheduleStatus.RECRUITING_SCHEDULE,ScheduleStatus.CONFIRMED_SCHEDULE,ScheduleStatus.CLOSED_SCHEDULE)
                .contains(schedule.getStatus())) throw new BusinessException(409,"INVALID_STATE","진행·완료 일정은 취소할 수 없습니다.");
        var applications=this.applications.findValidApplicationsByScheduleId(scheduleId);
        schedule.setStatus(ScheduleStatus.CANCELLED_SCHEDULE);
        schedule.setCancelledAt(java.time.LocalDateTime.ofInstant(clock.instant(),java.time.ZoneOffset.UTC));
        for(var application:applications) {
            application.setStatus(ApplicationStatus.CANCELLED);
            application.setCancelledAt(schedule.getCancelledAt());
            notifications.createNotification(application,NotificationType.SCHEDULE_CANCELLED);
        }
        schedule.setApplicantCount(0);
        return getScheduleResponse(schedule);
    }

    public TravelScheduleResponse getScheduleResponse(TravelSchedule schedule) {
        var response=TravelScheduleResponse.from(schedule);
        if(!response.selectionEnabled()) return response.withApplicationAvailability(false,"SCHEDULE_UNAVAILABLE");
        try {
            policy.requirePricing();
            if(!policy.withinDeadline(schedule.getDepartureDate(),clock))
                return response.withApplicationAvailability(false,"BOOKING_CLOSED");
            String theme=schedule.getTravelProduct().getTheme();
            int capacity=switch(theme) {case "허니문" -> 4;case "골프" -> 10;case "효도","트레킹" -> 20;default -> 0;};
            if("허니문".equals(theme)) policy.validHoneymoonSize(2);
            return response.withApplicationAvailability(schedule.getApplicantCount()<capacity,
                schedule.getApplicantCount()<capacity ? null : "CAPACITY_EXCEEDED");
        } catch(BusinessException e) {return response.withApplicationAvailability(false,e.getCode());}
    }

}

package com.misterworld.server.travelschedule.dto;

import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.domain.TravelSchedule;


import java.time.LocalDate;

public record TravelScheduleResponse(
    Long travelScheduleId,
    Long productId,
    LocalDate departureDate,
    LocalDate arrivalDate,
    ScheduleStatus status,
    Integer applicantCount,
    boolean applicationEnabled,
    String disabledReason,
    boolean selectionEnabled
) {
    public static TravelScheduleResponse from(TravelSchedule schedule) {
        return new TravelScheduleResponse(schedule.getTravelScheduleId(), schedule.getTravelProduct().getId(),
            schedule.getDepartureDate(), schedule.getArrivalDate(), schedule.getStatus(), schedule.getApplicantCount(),
            false, "POLICY_UNRESOLVED", schedule.getStatus() == ScheduleStatus.RECRUITING_SCHEDULE ||
                schedule.getStatus() == ScheduleStatus.CONFIRMED_SCHEDULE);
    }
    public TravelScheduleResponse withApplicationAvailability(boolean enabled,String reason) {
        return new TravelScheduleResponse(travelScheduleId,productId,departureDate,arrivalDate,status,
            applicantCount,enabled,reason,selectionEnabled);
    }

}

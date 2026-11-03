package com.misterworld.server.dto;

import com.misterworld.server.domain.*;

import java.time.LocalDate;

public record TravelScheduleResponse(
    Long travelScheduleId,
    Long productId,
    LocalDate departureDate,
    LocalDate arrivalDate,
    ScheduleStatus status,
    Integer applicantCount,
    boolean applicationEnabled,
    String disabledReason
) {
    public static TravelScheduleResponse from(TravelSchedule schedule) {
        return new TravelScheduleResponse(schedule.getTravelScheduleId(), schedule.getTravelProduct().getId(),
            schedule.getDepartureDate(), schedule.getArrivalDate(), schedule.getStatus(), schedule.getApplicantCount(),
            false, "POLICY_UNRESOLVED");
    }
}

package com.misterworld.server.travelschedule.dto;

import com.misterworld.server.travelschedule.entity.ScheduleStatus;
import java.time.LocalDate;
/** PDF p.32 management response; preserves the existing schedule JSON contract. */
public record ScheduleResponse(Long travelScheduleId,Long productId,LocalDate departureDate,
    LocalDate arrivalDate,ScheduleStatus status,Integer applicantCount,boolean applicationEnabled,
    String disabledReason,boolean selectionEnabled) {
    public static ScheduleResponse from(TravelScheduleResponse response) {
        return new ScheduleResponse(response.travelScheduleId(),response.productId(),response.departureDate(),
            response.arrivalDate(),response.status(),response.applicantCount(),response.applicationEnabled(),
            response.disabledReason(),response.selectionEnabled());
    }
}

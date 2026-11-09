package com.misterworld.server.dto;

import com.misterworld.server.domain.*;

import java.time.*;

public record ApplicationResponse(
    Long id,
    Long scheduleId,
    String tourGrade,
    String hotelOption,
    String transportOption,
    String mealOption,
    String extraOption,
    Integer applicantCount,
    Integer basePrice,
    Integer additionalOptionPrice,
    Integer discountRate,
    Integer discountAmount,
    Integer totalPrice,
    ApplicationStatus status,
    LocalDateTime appliedAt
) {
    public static ApplicationResponse from(TravelApplication a) {
        return new ApplicationResponse(a.getId(), a.getTravelSchedule().getTravelScheduleId(), a.getTourGrade(),
            a.getHotelOption(), a.getTransportOption(), a.getMealOption(), a.getExtraOption(), a.getApplicantCount(),
            a.getBasePrice(), a.getAdditionalOptionPrice(), a.getDiscountRate(), a.getDiscountAmount(),
            a.getTotalPrice(), a.getStatus(), a.getAppliedAt());
    }
}

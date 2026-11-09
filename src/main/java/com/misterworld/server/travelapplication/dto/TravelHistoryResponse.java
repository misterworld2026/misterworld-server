package com.misterworld.server.travelapplication.dto;

import com.misterworld.server.travelapplication.entity.ApplicationStatus;
import com.misterworld.server.travelapplication.entity.TravelApplication;


import java.time.LocalDate;

public record TravelHistoryResponse(
    Long id, Long scheduleId, Long productId, String productName,
    LocalDate departureDate, LocalDate arrivalDate, String tourGrade,
    String hotelOption, String transportOption, String mealOption, String extraOption,
    Integer applicantCount, Integer basePrice, Integer additionalOptionPrice,
    Integer discountRate, Integer discountAmount, Integer totalPrice,
    ApplicationStatus status, java.time.LocalDateTime appliedAt
) {
    public static TravelHistoryResponse from(TravelApplication a) {
        var schedule = a.getTravelSchedule();
        var product = schedule.getTravelProduct();
        return new TravelHistoryResponse(a.getId(), schedule.getTravelScheduleId(), product.getId(),
            product.getName(), schedule.getDepartureDate(), schedule.getArrivalDate(), a.getTourGrade(),
            a.getHotelOption(), a.getTransportOption(), a.getMealOption(), a.getExtraOption(),
            a.getApplicantCount(), a.getBasePrice(), a.getAdditionalOptionPrice(), a.getDiscountRate(),
            a.getDiscountAmount(), a.getTotalPrice(), a.getStatus(), a.getAppliedAt());
    }
}

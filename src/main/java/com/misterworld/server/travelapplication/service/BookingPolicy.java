package com.misterworld.server.travelapplication.service;

import com.misterworld.server.common.BusinessException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.RoundingMode;
import java.time.*;

/** Additional configuration: unresolved business rules have no implicit defaults. */
@Component
public class BookingPolicy {
    private final String pricingUnit, rounding, zone, honeymoonSize, cancelAfterDeadline;
    public BookingPolicy(@Value("${app.booking.pricing-unit:}") String pricingUnit,
            @Value("${app.booking.discount-rounding:}") String rounding,
            @Value("${app.booking.zone:}") String zone,
            @Value("${app.booking.honeymoon-size:}") String honeymoonSize,
            @Value("${app.booking.cancel-after-deadline:}") String cancelAfterDeadline) {
        this.pricingUnit=pricingUnit; this.rounding=rounding; this.zone=zone;
        this.honeymoonSize=honeymoonSize; this.cancelAfterDeadline=cancelAfterDeadline;
    }
    public void requirePricing() {
        if (!"PER_PERSON_SURCHARGE".equals(pricingUnit) || rounding.isBlank()) throw BusinessException.policy();
    }
    public RoundingMode discountRounding() { requirePricing(); return RoundingMode.valueOf(rounding); }
    public LocalDate today(Clock clock) {
        if (zone.isBlank()) throw BusinessException.policy();
        return LocalDate.now(clock.withZone(ZoneId.of(zone)));
    }
    public boolean withinDeadline(LocalDate departureDate, Clock clock) {
        return !departureDate.isBefore(firstBookableDeparture(today(clock)));
    }
    public LocalDate firstBookableDeparture(LocalDate today) {
        // Explicit configured policy: D-7 local calendar day inclusive.
        return today.plusDays(7);
    }
    public boolean validHoneymoonSize(int applicantCount) {
        if (honeymoonSize.isBlank()) throw BusinessException.policy();
        return switch(honeymoonSize) { case "ONE_TEAM" -> applicantCount==2;
            case "UP_TO_TWO_TEAMS" -> applicantCount==2 || applicantCount==4;
            default -> throw BusinessException.policy(); };
    }
    public boolean cancelAfterDeadline() {
        if (!"true".equals(cancelAfterDeadline) && !"false".equals(cancelAfterDeadline)) throw BusinessException.policy();
        return Boolean.parseBoolean(cancelAfterDeadline);
    }
}

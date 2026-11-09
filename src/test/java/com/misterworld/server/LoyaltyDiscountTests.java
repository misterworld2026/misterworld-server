package com.misterworld.server;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.service.TravelApplicationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoyaltyDiscountTests {
    @Test
    void discountStartsAfterTwoCompletedTrips() {
        var service = new TravelApplicationService(null);

        assertThat(service.applyLoyaltyDiscount(0)).isZero();
        assertThat(service.applyLoyaltyDiscount(1)).isZero();
        assertThat(service.applyLoyaltyDiscount(2)).isEqualTo(10);
        assertThat(service.applyLoyaltyDiscount(3)).isEqualTo(10);
        assertThatThrownBy(() -> service.applyLoyaltyDiscount(-1))
            .isInstanceOf(BusinessException.class);
    }
}

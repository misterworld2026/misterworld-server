package com.misterworld.server.dto;

import com.misterworld.server.domain.TravelProduct;

import java.util.List;

public record TravelProductResponse(
    Long id,
    String name,
    String theme,
    String description,
    Integer classicPrice,
    Integer grandPrice,
    Integer premiumPrice,
    List<TravelOptionResponse> options
) {
    public static TravelProductResponse from(TravelProduct p, List<TravelOptionResponse> options) {
        return new TravelProductResponse(p.getId(), p.getName(), p.getTheme(), p.getDescription(), p.getClassicPrice(),
            p.getGrandPrice(), p.getPremiumPrice(), options);
    }
}

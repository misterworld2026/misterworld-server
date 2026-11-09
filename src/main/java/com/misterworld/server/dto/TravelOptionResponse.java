package com.misterworld.server.dto;

import com.misterworld.server.domain.TravelOption;

public record TravelOptionResponse(
    Long id,
    String type,
    String name,
    String grade,
    Integer additionalPrice,
    String description,
    String defaultForGrade
) {
    public static TravelOptionResponse from(TravelOption o) {
        return new TravelOptionResponse(o.getId(), o.getType(), o.getName(), o.getGrade(), o.getAdditionalPrice(),
            o.getDescription(), o.getDefaultForGrade());
    }
}

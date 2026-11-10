package com.misterworld.server.travelproduct.dto;

import com.misterworld.server.domain.TravelOption;


public record TravelOptionResponse(
    Long id,
    String type,
    String name,
    String grade,
    Integer additionalPrice,
    String description,
    String defaultForGrade,
    java.util.List<String> allowedTourGrades,
    String benefitCode
) {
    public static TravelOptionResponse from(TravelOption o) {
        return new TravelOptionResponse(o.getId(), o.getType(), o.getName(), o.getGrade(), o.getAdditionalPrice(),
            o.getDescription(), o.getDefaultForGrade(),
            o.getAllowedTourGrades() == null ? java.util.List.of() :
                java.util.Arrays.stream(o.getAllowedTourGrades().split(",")).map(String::trim)
                    .filter(s -> !s.isEmpty()).distinct().toList(), o.getBenefitCode());
    }
}

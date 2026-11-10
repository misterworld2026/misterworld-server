package com.misterworld.server.travelproduct.dto;

import jakarta.validation.constraints.*;

public record TravelOptionRequest(
    Long id,
    @NotBlank String type,
    @NotBlank @Size(max = 255) String name,
    @NotBlank String grade,
    @NotNull @Min(0) Integer additionalPrice,
    @Size(max = 2000) String description,
    String defaultForGrade,
    java.util.List<@NotBlank String> allowedTourGrades,
    @Size(max=40) String benefitCode
) {
    public TravelOptionRequest(Long id,String type,String name,String grade,Integer additionalPrice,
            String description,String defaultForGrade) {
        this(id,type,name,grade,additionalPrice,description,defaultForGrade,null,null);
    }
}

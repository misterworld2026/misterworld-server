package com.misterworld.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TravelProductResponse {

    private Long id;

    private String name;

    private String theme;

    private String description;

    private Integer classPrice;

    private Integer grandPrice;

    private Integer premiumPrice;
}

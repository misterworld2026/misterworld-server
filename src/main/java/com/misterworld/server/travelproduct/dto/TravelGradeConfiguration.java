package com.misterworld.server.travelproduct.dto;

import java.util.List;

/** Additional response structure for the design's productConfiguration. */
public record TravelGradeConfiguration(String grade, Integer price,
    List<TravelOptionResponse> defaultOptions, List<TravelOptionResponse> allowedOptions,
    boolean configurationReady) {}

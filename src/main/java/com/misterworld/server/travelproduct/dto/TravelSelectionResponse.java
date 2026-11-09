package com.misterworld.server.travelproduct.dto;

import com.misterworld.server.travelschedule.dto.TravelScheduleResponse;


import java.util.List;

public record TravelSelectionResponse(TravelProductResponse product, TravelScheduleResponse schedule,
    String grade, List<TravelOptionResponse> defaultOptions,
    List<TravelOptionResponse> selectedOptions, List<TravelOptionResponse> changedOptions) {}

package com.misterworld.server.travelapplication.dto;

import com.misterworld.server.travelproduct.dto.TravelOptionResponse;
import com.misterworld.server.travelproduct.dto.TravelProductResponse;
import com.misterworld.server.travelschedule.dto.TravelScheduleResponse;
import java.util.List;
import java.time.Instant;
public record TravelQuoteResponse(TravelProductResponse product, TravelScheduleResponse schedule,
    String grade, int applicantCount, List<TravelOptionResponse> selectedOptions,
    long completedTrips, TravelPriceResponse price, String quoteToken, Instant expiresAt) {}

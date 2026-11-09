package com.misterworld.server.travelapplication.dto;
/** Additional exact-money response shared by quote and saved booking. */
public record TravelPriceResponse(int basePrice, int additionalOptionPrice, int discountRate,
    int discountAmount, int totalPrice) {}

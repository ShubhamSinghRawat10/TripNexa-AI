package com.tripnexa.smarttrip.agent.result;

public record HotelOption(
    String name,
    long pricePerNight,
    double rating,
    String category
) {}

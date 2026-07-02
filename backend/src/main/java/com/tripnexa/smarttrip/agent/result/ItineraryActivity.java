package com.tripnexa.smarttrip.agent.result;

public record ItineraryActivity(
    String name,
    String startTime,
    String endTime,
    long estimatedCost,
    String category
) {}

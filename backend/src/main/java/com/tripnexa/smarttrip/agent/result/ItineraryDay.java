package com.tripnexa.smarttrip.agent.result;

import java.util.List;

public record ItineraryDay(
    int day,
    List<ItineraryActivity> activities
) {}

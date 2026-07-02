package com.tripnexa.smarttrip.agent.result;

import com.tripnexa.smarttrip.entity.CrowdLevel;
import java.time.LocalDate;

public record TripRecommendation(
    int tripScore,
    HotelOption bestHotel,
    CrowdLevel crowdLevel,
    LocalDate bestTravelDate,
    long estimatedSavings,
    String verdict
) {}

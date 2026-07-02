package com.tripnexa.smarttrip.dto.trip;

import com.tripnexa.smarttrip.agent.result.CrowdResult;
import com.tripnexa.smarttrip.agent.result.PriceResult;
import com.tripnexa.smarttrip.agent.result.TripRecommendation;

public record ShouldITravelResponse(
    String destination,
    CrowdResult crowd,
    PriceResult price,
    TripRecommendation recommendation
) {}

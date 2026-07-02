package com.tripnexa.smarttrip.dto.trip;

import com.tripnexa.smarttrip.agent.result.BudgetBreakdown;
import com.tripnexa.smarttrip.agent.result.HotelResult;
import com.tripnexa.smarttrip.agent.result.ItineraryDay;
import com.tripnexa.smarttrip.agent.result.TripRecommendation;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GenerateResponse(
    UUID tripId,
    String destination,
    LocalDate travelDate,
    int days,
    int people,
    List<ItineraryDay> itinerary,
    BudgetBreakdown budget,
    HotelResult hotels,
    TripRecommendation recommendation,
    List<String> weatherWarnings
) {}

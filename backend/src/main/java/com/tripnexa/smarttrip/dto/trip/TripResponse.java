package com.tripnexa.smarttrip.dto.trip;

import com.tripnexa.smarttrip.entity.Trip;
import com.tripnexa.smarttrip.entity.TripStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record TripResponse(
    UUID id,
    String destination,
    long budget,
    int days,
    int people,
    LocalDate travelDate,
    Set<String> interests,
    TripStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
            trip.getId(), trip.getDestination(), trip.getBudget(), trip.getDays(),
            trip.getPeople(), trip.getTravelDate(), trip.getInterests(), trip.getStatus(),
            trip.getCreatedAt(), trip.getUpdatedAt()
        );
    }
}

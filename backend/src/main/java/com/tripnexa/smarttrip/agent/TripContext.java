package com.tripnexa.smarttrip.agent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Immutable, request-scoped context shared across all agents.
 * Built once per trip generation request — agents read from it but never mutate it.
 */
public record TripContext(
    UUID tripId,
    UUID userId,
    String destination,
    LocalDate travelDate,
    int days,
    int people,
    long totalBudget,
    List<String> interests,
    String correlationId
) {}

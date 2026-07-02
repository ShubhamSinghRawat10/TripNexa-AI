package com.tripnexa.smarttrip.agent.result;

public record BudgetBreakdown(
    long hotel,
    long food,
    long transport,
    long activities,
    long total,
    boolean feasible,
    String note
) {}

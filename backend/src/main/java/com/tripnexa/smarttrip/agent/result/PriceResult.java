package com.tripnexa.smarttrip.agent.result;

public record PriceResult(
    long currentPrice,
    long baselinePrice,
    int surgePercent,
    boolean isPeakDate
) {}

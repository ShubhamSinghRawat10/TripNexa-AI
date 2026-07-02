package com.tripnexa.smarttrip.agent.result;

import com.tripnexa.smarttrip.entity.CrowdLevel;

public record CrowdResult(
    CrowdLevel level,
    String reason
) {
    public static CrowdResult defaultLow() {
        return new CrowdResult(CrowdLevel.LOW, "No peak event detected");
    }
}

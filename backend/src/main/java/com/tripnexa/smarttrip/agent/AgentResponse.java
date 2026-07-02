package com.tripnexa.smarttrip.agent;

/**
 * Uniform wrapper for every agent result so the Planner can inspect
 * success/failure without try-catch scattered everywhere.
 */
public record AgentResponse<T>(
    boolean success,
    T data,
    String errorMessage,
    long executionTimeMs
) {
    public static <T> AgentResponse<T> ok(T data, long ms) {
        return new AgentResponse<>(true, data, null, ms);
    }

    public static <T> AgentResponse<T> failed(String error, long ms) {
        return new AgentResponse<>(false, null, error, ms);
    }
}

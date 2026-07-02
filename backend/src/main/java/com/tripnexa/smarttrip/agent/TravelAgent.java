package com.tripnexa.smarttrip.agent;

/**
 * Core contract for every travel agent. Each agent takes a typed input,
 * has access to the shared TripContext, and returns a uniform AgentResponse.
 */
public interface TravelAgent<I, O> {
    AgentResponse<O> execute(I input, TripContext context);
}

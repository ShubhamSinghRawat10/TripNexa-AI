package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.BudgetBreakdown;
import com.tripnexa.smarttrip.agent.result.ItineraryDay;
import com.tripnexa.smarttrip.integration.gemini.GeminiClient;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Generates a day-wise itinerary by calling Gemini API.
 * Takes weather warnings and budget into account.
 */
@Component
public class ItineraryAgent implements TravelAgent<ItineraryAgent.ItineraryInput, List<ItineraryDay>> {

    public record ItineraryInput(BudgetBreakdown budget, List<String> weatherWarnings) {}

    private final GeminiClient geminiClient;

    public ItineraryAgent(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    @Override
    public AgentResponse<List<ItineraryDay>> execute(ItineraryInput input, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            long activityBudget = input.budget() != null ? input.budget().activities() : context.totalBudget() / 4;
            List<String> warnings = input.weatherWarnings() != null ? input.weatherWarnings() : List.of();

            List<ItineraryDay> itinerary = geminiClient.generateItinerary(
                context.destination(), context.days(), activityBudget,
                context.interests(), warnings);

            return AgentResponse.ok(itinerary, elapsed(start));

        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

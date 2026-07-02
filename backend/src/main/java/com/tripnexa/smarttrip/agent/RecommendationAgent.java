package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.BudgetBreakdown;
import com.tripnexa.smarttrip.agent.result.CrowdResult;
import com.tripnexa.smarttrip.agent.result.HotelResult;
import com.tripnexa.smarttrip.agent.result.PriceResult;
import com.tripnexa.smarttrip.agent.result.TripRecommendation;
import com.tripnexa.smarttrip.entity.CrowdLevel;
import com.tripnexa.smarttrip.util.HolidayUtil;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Synthesises all agent results into a final trip score, best travel date,
 * estimated savings, and a "should I travel now?" verdict.
 */
@Component
public class RecommendationAgent
    implements TravelAgent<RecommendationAgent.RecommendationInput, TripRecommendation> {

    public record RecommendationInput(CrowdResult crowd, PriceResult price,
                                       HotelResult hotel, BudgetBreakdown budget) {}

    @Override
    public AgentResponse<TripRecommendation> execute(RecommendationInput input, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            int score = 100;
            StringBuilder verdict = new StringBuilder();

            // --- Crowd scoring ---
            CrowdLevel crowd = input.crowd() != null ? input.crowd().level() : CrowdLevel.LOW;
            switch (crowd) {
                case HIGH -> { score -= 25; verdict.append("⚠️ High crowd expected: ").append(input.crowd().reason()).append(". "); }
                case MEDIUM -> { score -= 10; verdict.append("Moderate crowd expected. "); }
                default -> verdict.append("Low crowds — great time to visit! ");
            }

            // --- Price scoring ---
            int surgePercent = input.price() != null ? input.price().surgePercent() : 0;
            if (surgePercent > 30) {
                score -= 20;
                verdict.append("Prices are ").append(surgePercent).append("% above baseline. ");
            } else if (surgePercent > 0) {
                score -= 10;
                verdict.append("Slight price surge of ").append(surgePercent).append("%. ");
            } else {
                verdict.append("Prices are at baseline — good deal! ");
            }

            // --- Budget feasibility ---
            if (input.budget() != null && !input.budget().feasible()) {
                score -= 15;
                verdict.append("Budget is tight for this trip. ");
            }

            score = Math.max(10, Math.min(100, score));

            // --- Find best alternative date (up to 14 days out) ---
            LocalDate bestDate = context.travelDate();
            long baseSavings = 0;
            if (input.price() != null && input.price().isPeakDate()) {
                long savings = input.price().currentPrice() - input.price().baselinePrice();
                for (int offset = 1; offset <= 14; offset++) {
                    LocalDate candidate = context.travelDate().plusDays(offset);
                    if (!HolidayUtil.isPeakDate(candidate)) {
                        bestDate = candidate;
                        baseSavings = savings * context.days();
                        break;
                    }
                }
            }

            // Build the final verdict
            if (baseSavings > 0) {
                verdict.append(String.format("Consider shifting to %s — you'd save about ₹%,d.", bestDate, baseSavings));
            }

            return AgentResponse.ok(
                new TripRecommendation(
                    score,
                    input.hotel() != null ? input.hotel().recommended() : null,
                    crowd,
                    bestDate,
                    baseSavings,
                    verdict.toString().trim()),
                elapsed(start));

        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

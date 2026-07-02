package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.BudgetBreakdown;
import com.tripnexa.smarttrip.agent.result.HotelResult;
import com.tripnexa.smarttrip.agent.result.PriceResult;
import org.springframework.stereotype.Component;

/**
 * Splits the total budget into hotel/food/transport/activities.
 * Uses the recommended hotel price from HotelAgent and current pricing from PriceAgent.
 */
@Component
public class BudgetAgent implements TravelAgent<BudgetAgent.BudgetInput, BudgetBreakdown> {

    public record BudgetInput(long totalBudget, int days, int people,
                              HotelResult hotelResult, PriceResult priceResult) {}

    @Override
    public AgentResponse<BudgetBreakdown> execute(BudgetInput input, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            long total = input.totalBudget();
            int days = input.days();
            int people = input.people();

            // Hotel: use recommended hotel price * days
            long hotelPerNight = input.hotelResult() != null && input.hotelResult().recommended() != null
                ? input.hotelResult().recommended().pricePerNight()
                : (input.priceResult() != null ? input.priceResult().currentPrice() : 3000);
            long hotel = hotelPerNight * days;

            // Remaining budget after hotel
            long remaining = total - hotel;
            if (remaining < 0) {
                // Hotel alone exceeds budget — squeeze allocations
                hotel = (long) (total * 0.45);
                remaining = total - hotel;
            }

            // Allocate remaining: food 40%, transport 30%, activities 30%
            long food = Math.round(remaining * 0.40);
            long transport = Math.round(remaining * 0.30);
            long activities = remaining - food - transport;

            boolean feasible = total >= (hotel + food + transport + activities);
            String note = feasible
                ? String.format("Budget of ₹%,d is sufficient for %d days, %d people", total, days, people)
                : String.format("Budget of ₹%,d is tight — consider increasing by ₹%,d",
                    total, (hotel + food + transport + activities) - total);

            return AgentResponse.ok(
                new BudgetBreakdown(hotel, food, transport, activities,
                    hotel + food + transport + activities, feasible, note),
                elapsed(start));

        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

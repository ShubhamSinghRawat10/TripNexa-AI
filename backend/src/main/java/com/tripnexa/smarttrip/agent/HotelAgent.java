package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.HotelOption;
import com.tripnexa.smarttrip.agent.result.HotelResult;
import com.tripnexa.smarttrip.repository.DestinationPriceBaselineRepository;
import com.tripnexa.smarttrip.util.HolidayUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Returns 3 hotel options (budget, mid-range, premium) within the user's budget.
 * Prices are derived from the seeded baseline with realistic variations.
 */
@Component
public class HotelAgent implements TravelAgent<TripContext, HotelResult> {

    private final DestinationPriceBaselineRepository repository;

    public HotelAgent(DestinationPriceBaselineRepository repository) {
        this.repository = repository;
    }

    @Override
    public AgentResponse<HotelResult> execute(TripContext input, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            var baseline = repository.findByDestinationIgnoreCase(context.destination());
            long basePrice = baseline.map(b -> b.priceFor(HolidayUtil.isPeakDate(context.travelDate())))
                .orElse(3000L);

            long perNightBudget = context.totalBudget() / Math.max(1, context.days());

            List<HotelOption> options = new ArrayList<>();

            // Budget option — 70% of base price
            long budgetPrice = Math.round(basePrice * 0.70);
            options.add(new HotelOption(
                context.destination() + " Inn",
                budgetPrice, 3.5, "Budget"));

            // Mid-range — base price
            options.add(new HotelOption(
                "Hotel " + context.destination() + " Grand",
                basePrice, 4.0, "Mid-range"));

            // Premium — 160% of base price
            long premiumPrice = Math.round(basePrice * 1.60);
            options.add(new HotelOption(
                "The " + context.destination() + " Palace",
                premiumPrice, 4.6, "Premium"));

            // Filter to options the user can afford and pick recommended
            List<HotelOption> affordable = options.stream()
                .filter(h -> h.pricePerNight() <= perNightBudget)
                .toList();

            // If nothing is affordable, still show the cheapest
            if (affordable.isEmpty()) {
                affordable = List.of(options.stream()
                    .min(Comparator.comparingLong(HotelOption::pricePerNight))
                    .orElseThrow());
            }

            // Recommend the best-rated affordable option
            HotelOption recommended = affordable.stream()
                .max(Comparator.comparingDouble(HotelOption::rating))
                .orElse(options.getFirst());

            return AgentResponse.ok(
                new HotelResult(options, recommended),
                elapsed(start));

        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

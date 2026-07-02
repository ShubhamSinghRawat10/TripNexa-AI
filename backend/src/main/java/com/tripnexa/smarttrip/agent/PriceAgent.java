package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.PriceResult;
import com.tripnexa.smarttrip.repository.DestinationPriceBaselineRepository;
import com.tripnexa.smarttrip.util.HolidayUtil;
import org.springframework.stereotype.Component;

/**
 * Compares current hotel price vs. baseline for the destination.
 * Uses peak-date logic to calculate surge pricing.
 */
@Component
public class PriceAgent implements TravelAgent<TripContext, PriceResult> {

    private final DestinationPriceBaselineRepository repository;

    public PriceAgent(DestinationPriceBaselineRepository repository) {
        this.repository = repository;
    }

    @Override
    public AgentResponse<PriceResult> execute(TripContext input, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            var baseline = repository.findByDestinationIgnoreCase(context.destination());
            if (baseline.isEmpty()) {
                // Unknown destination — return a reasonable default
                return AgentResponse.ok(
                    new PriceResult(3000, 3000, 0, false),
                    elapsed(start));
            }

            var dest = baseline.get();
            boolean isPeak = HolidayUtil.isPeakDate(context.travelDate());
            long currentPrice = dest.priceFor(isPeak);
            long baselinePrice = dest.getAverageHotelPrice();
            int surgePercent = baselinePrice > 0
                ? (int) ((currentPrice - baselinePrice) * 100 / baselinePrice)
                : 0;

            return AgentResponse.ok(
                new PriceResult(currentPrice, baselinePrice, surgePercent, isPeak),
                elapsed(start));

        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.CrowdResult;
import com.tripnexa.smarttrip.entity.CrowdLevel;
import com.tripnexa.smarttrip.repository.DestinationCrowdCalendarRepository;
import com.tripnexa.smarttrip.util.HolidayUtil;
import org.springframework.stereotype.Component;

/**
 * Estimates crowd level for a destination on a given date.
 * Checks the seeded crowd calendar first, then falls back to HolidayUtil
 * for dynamic weekend/holiday detection.
 */
@Component
public class CrowdAgent implements TravelAgent<TripContext, CrowdResult> {

    private final DestinationCrowdCalendarRepository repository;

    public CrowdAgent(DestinationCrowdCalendarRepository repository) {
        this.repository = repository;
    }

    @Override
    public AgentResponse<CrowdResult> execute(TripContext input, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            // Check seeded calendar first (holidays/festivals)
            var record = repository.findByIdDestinationIgnoreCaseAndIdTravelDate(
                context.destination(), context.travelDate());

            if (record.isPresent()) {
                var cal = record.get();
                return AgentResponse.ok(
                    new CrowdResult(cal.getCrowdLevel(), cal.getReason()),
                    elapsed(start));
            }

            // Fall back to dynamic weekend detection
            if (HolidayUtil.isWeekend(context.travelDate())) {
                return AgentResponse.ok(
                    new CrowdResult(CrowdLevel.MEDIUM, "Weekend"),
                    elapsed(start));
            }

            // Check if any day in the trip range hits a peak date
            for (int d = 1; d < context.days(); d++) {
                var date = context.travelDate().plusDays(d);
                if (HolidayUtil.isPeakDate(date)) {
                    return AgentResponse.ok(
                        new CrowdResult(CrowdLevel.MEDIUM,
                            "Trip overlaps peak date: " + HolidayUtil.peakReason(date) + " on " + date),
                        elapsed(start));
                }
            }

            return AgentResponse.ok(CrowdResult.defaultLow(), elapsed(start));

        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

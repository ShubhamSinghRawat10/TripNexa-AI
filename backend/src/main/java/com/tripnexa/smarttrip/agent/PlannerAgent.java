package com.tripnexa.smarttrip.agent;

import com.tripnexa.smarttrip.agent.result.BudgetBreakdown;
import com.tripnexa.smarttrip.agent.result.CrowdResult;
import com.tripnexa.smarttrip.agent.result.HotelResult;
import com.tripnexa.smarttrip.agent.result.ItineraryDay;
import com.tripnexa.smarttrip.agent.result.PriceResult;
import com.tripnexa.smarttrip.agent.result.TripRecommendation;
import com.tripnexa.smarttrip.dto.trip.GenerateResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Orchestrator — fans out independent agents in parallel (Stage 1),
 * then sequentially runs dependent agents (Stages 2-4).
 */
@Component
public class PlannerAgent {

    private static final Logger log = LoggerFactory.getLogger(PlannerAgent.class);

    private final CrowdAgent crowdAgent;
    private final PriceAgent priceAgent;
    private final HotelAgent hotelAgent;
    private final BudgetAgent budgetAgent;
    private final ItineraryAgent itineraryAgent;
    private final RecommendationAgent recommendationAgent;
    private final Executor agentExecutor;

    public PlannerAgent(CrowdAgent crowdAgent, PriceAgent priceAgent,
                        HotelAgent hotelAgent, BudgetAgent budgetAgent,
                        ItineraryAgent itineraryAgent, RecommendationAgent recommendationAgent,
                        @Qualifier("agentExecutor") Executor agentExecutor) {
        this.crowdAgent = crowdAgent;
        this.priceAgent = priceAgent;
        this.hotelAgent = hotelAgent;
        this.budgetAgent = budgetAgent;
        this.itineraryAgent = itineraryAgent;
        this.recommendationAgent = recommendationAgent;
        this.agentExecutor = agentExecutor;
    }

    public GenerateResponse plan(TripContext ctx) {
        log.info("[{}] Starting trip generation for {} on {}",
            ctx.correlationId(), ctx.destination(), ctx.travelDate());

        // ===== Stage 1: Independent agents — fan out in parallel =====
        var crowdFuture = CompletableFuture.supplyAsync(
            () -> crowdAgent.execute(ctx, ctx), agentExecutor);
        var priceFuture = CompletableFuture.supplyAsync(
            () -> priceAgent.execute(ctx, ctx), agentExecutor);
        var hotelFuture = CompletableFuture.supplyAsync(
            () -> hotelAgent.execute(ctx, ctx), agentExecutor);

        CompletableFuture.allOf(crowdFuture, priceFuture, hotelFuture).join();

        var crowdResponse = crowdFuture.join();
        var priceResponse = priceFuture.join();
        var hotelResponse = hotelFuture.join();

        log.info("[{}] Stage 1 complete — Crowd: {}ms, Price: {}ms, Hotel: {}ms",
            ctx.correlationId(),
            crowdResponse.executionTimeMs(), priceResponse.executionTimeMs(), hotelResponse.executionTimeMs());

        CrowdResult crowdResult = crowdResponse.success() ? crowdResponse.data() : CrowdResult.defaultLow();
        PriceResult priceResult = priceResponse.success() ? priceResponse.data() : new PriceResult(3000, 3000, 0, false);
        HotelResult hotelResult = hotelResponse.success() ? hotelResponse.data() : null;

        // ===== Stage 2: Budget agent — depends on hotel + price =====
        var budgetResponse = budgetAgent.execute(
            new BudgetAgent.BudgetInput(ctx.totalBudget(), ctx.days(), ctx.people(), hotelResult, priceResult),
            ctx);

        log.info("[{}] Stage 2 complete — Budget: {}ms", ctx.correlationId(), budgetResponse.executionTimeMs());

        BudgetBreakdown budgetResult = budgetResponse.success() ? budgetResponse.data() : null;

        // ===== Stage 3: Itinerary agent — calls Gemini (slowest step) =====
        var itineraryResponse = itineraryAgent.execute(
            new ItineraryAgent.ItineraryInput(budgetResult, List.of()),
            ctx);

        log.info("[{}] Stage 3 complete — Itinerary: {}ms", ctx.correlationId(), itineraryResponse.executionTimeMs());

        List<ItineraryDay> itinerary = itineraryResponse.success() ? itineraryResponse.data() : List.of();

        // ===== Stage 4: Recommendation agent — depends on everything =====
        var recResponse = recommendationAgent.execute(
            new RecommendationAgent.RecommendationInput(crowdResult, priceResult, hotelResult, budgetResult),
            ctx);

        log.info("[{}] Stage 4 complete — Recommendation: {}ms", ctx.correlationId(), recResponse.executionTimeMs());

        TripRecommendation recommendation = recResponse.success() ? recResponse.data() : null;

        // ===== Build final response =====
        return new GenerateResponse(
            ctx.tripId(),
            ctx.destination(),
            ctx.travelDate(),
            ctx.days(),
            ctx.people(),
            itinerary,
            budgetResult,
            hotelResult,
            recommendation,
            List.of()  // weather warnings (optional if WeatherAgent is configured)
        );
    }
}

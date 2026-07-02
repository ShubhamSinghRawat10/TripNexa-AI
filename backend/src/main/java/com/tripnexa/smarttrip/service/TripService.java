package com.tripnexa.smarttrip.service;

import com.tripnexa.smarttrip.agent.AgentResponse;
import com.tripnexa.smarttrip.agent.CrowdAgent;
import com.tripnexa.smarttrip.agent.PlannerAgent;
import com.tripnexa.smarttrip.agent.PriceAgent;
import com.tripnexa.smarttrip.agent.RecommendationAgent;
import com.tripnexa.smarttrip.agent.TripContext;
import com.tripnexa.smarttrip.agent.result.CrowdResult;
import com.tripnexa.smarttrip.agent.result.PriceResult;
import com.tripnexa.smarttrip.agent.result.TripRecommendation;
import com.tripnexa.smarttrip.dto.trip.GenerateResponse;
import com.tripnexa.smarttrip.dto.trip.ShouldITravelResponse;
import com.tripnexa.smarttrip.dto.trip.TripRequest;
import com.tripnexa.smarttrip.dto.trip.TripResponse;
import com.tripnexa.smarttrip.entity.Trip;
import com.tripnexa.smarttrip.exception.ResourceNotFoundException;
import com.tripnexa.smarttrip.repository.TripRepository;
import com.tripnexa.smarttrip.repository.UserRepository;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final PlannerAgent plannerAgent;
    private final CrowdAgent crowdAgent;
    private final PriceAgent priceAgent;
    private final RecommendationAgent recommendationAgent;

    public TripService(TripRepository tripRepository, UserRepository userRepository,
                       PlannerAgent plannerAgent, CrowdAgent crowdAgent,
                       PriceAgent priceAgent, RecommendationAgent recommendationAgent) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.plannerAgent = plannerAgent;
        this.crowdAgent = crowdAgent;
        this.priceAgent = priceAgent;
        this.recommendationAgent = recommendationAgent;
    }

    @Transactional
    public TripResponse create(UUID userId, TripRequest request) {
        var user = userRepository.getReferenceById(userId);
        Trip trip = new Trip(user, request.destination().trim(), request.budget(), request.days(),
            request.people(), request.travelDate(), normalizeInterests(request.interests()));
        return TripResponse.from(tripRepository.save(trip));
    }

    /**
     * Creates a trip draft and runs the full multi-agent pipeline to generate
     * an itinerary, budget breakdown, hotel options, and recommendation.
     */
    @Transactional
    public GenerateResponse generate(UUID userId, TripRequest request) {
        var user = userRepository.getReferenceById(userId);
        Trip trip = new Trip(user, request.destination().trim(), request.budget(), request.days(),
            request.people(), request.travelDate(), normalizeInterests(request.interests()));
        trip.markGenerated();
        trip = tripRepository.save(trip);

        TripContext context = new TripContext(
            trip.getId(), userId, trip.getDestination(), trip.getTravelDate(),
            trip.getDays(), trip.getPeople(), trip.getBudget(),
            List.copyOf(trip.getInterests()), UUID.randomUUID().toString().substring(0, 8));

        return plannerAgent.plan(context);
    }

    /**
     * Lightweight crowd + price + recommendation check — no itinerary generation.
     */
    public ShouldITravelResponse shouldITravel(String destination, long budget, LocalDate date) {
        TripContext ctx = new TripContext(
            null, null, destination, date, 3, 2, budget,
            List.of(), UUID.randomUUID().toString().substring(0, 8));

        AgentResponse<CrowdResult> crowdResponse = crowdAgent.execute(ctx, ctx);
        AgentResponse<PriceResult> priceResponse = priceAgent.execute(ctx, ctx);

        CrowdResult crowd = crowdResponse.success() ? crowdResponse.data() : CrowdResult.defaultLow();
        PriceResult price = priceResponse.success() ? priceResponse.data() : new PriceResult(3000, 3000, 0, false);

        AgentResponse<TripRecommendation> recResponse = recommendationAgent.execute(
            new RecommendationAgent.RecommendationInput(crowd, price, null, null), ctx);
        TripRecommendation recommendation = recResponse.success() ? recResponse.data() : null;

        return new ShouldITravelResponse(destination, crowd, price, recommendation);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> history(UUID userId) {
        return tripRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(TripResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse get(UUID userId, UUID tripId) {
        return TripResponse.from(ownedTrip(userId, tripId));
    }

    @Transactional
    public TripResponse update(UUID userId, UUID tripId, TripRequest request) {
        Trip trip = ownedTrip(userId, tripId);
        trip.update(request.destination().trim(), request.budget(), request.days(), request.people(),
            request.travelDate(), normalizeInterests(request.interests()));
        return TripResponse.from(trip);
    }

    @Transactional
    public void delete(UUID userId, UUID tripId) {
        tripRepository.delete(ownedTrip(userId, tripId));
    }

    private Trip ownedTrip(UUID userId, UUID tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private Set<String> normalizeInterests(Set<String> interests) {
        Set<String> normalized = new LinkedHashSet<>();
        interests.forEach(value -> normalized.add(value.trim().toLowerCase(Locale.ROOT)));
        return normalized;
    }
}

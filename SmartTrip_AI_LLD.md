# SmartTrip AI — Low-Level Design (LLD)

This document goes one level deeper than the earlier implementation plan: actual class contracts, method signatures, concurrency handling, design patterns, and request/response contracts. Read alongside the diagram shared in chat, which shows the runtime flow this document describes at the class level.

---

## 1. Layered architecture

```
Client (React)
   │  HTTP/JSON
   ▼
Controller layer      — TripController, AuthController
   │
   ▼
Service layer         — TripService (use-case orchestration, transactions)
   │
   ▼
Agent layer            — PlannerAgent + 7 specialized agents
   │
   ▼
Integration layer      — GeminiClient, WeatherClient, mock data repositories
   │
   ▼
Persistence layer      — Spring Data JPA repositories → PostgreSQL
```

Each layer only calls the layer directly below it. The Controller never touches an Agent directly, and no Agent ever touches a Repository directly — it goes through the Service or a dedicated data-access helper. This keeps every layer independently testable.

---

## 2. Core contracts

### 2.1 The agent interface (Strategy pattern)

Every agent implements the same generic contract. This is what makes the Planner able to treat all seven agents uniformly.

```java
public interface TravelAgent<I, O> {
    O execute(I input, TripContext context);
}
```

- `I` — the input type specific to that agent (e.g. `CrowdAgentInput`)
- `O` — the output type specific to that agent (e.g. `CrowdAgentResult`)
- `TripContext` — a shared, read-only object carrying request-wide data (destination, dates, budget, userId, correlationId for logging) so agents don't need to re-parse the original request

### 2.2 TripContext (shared state, built once per request)

```java
public record TripContext(
    UUID tripId,
    UUID userId,
    String destination,
    LocalDate travelDate,
    int days,
    int people,
    int totalBudget,
    List<String> interests,
    String correlationId
) {}
```

Immutable by design — agents read from it but never mutate it. This avoids race conditions when agents run in parallel (see §4).

### 2.3 AgentResponse wrapper

Every agent result is wrapped uniformly so the Planner and Recommendation Agent can inspect success/failure without try/catch scattered everywhere.

```java
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
```

---

## 3. Class-level design per agent

| Class | Input | Output | Depends on |
|---|---|---|---|
| `CrowdAgent` | destination, travelDate | `CrowdResult(level, reason)` | `CrowdCalendarRepository` |
| `PriceAgent` | destination, travelDate | `PriceResult(currentPrice, baselinePrice, surgePercent)` | `PriceBaselineRepository` |
| `WeatherAgent` | destination, travelDate, days | `WeatherResult(dailyForecast, warnings)` | `WeatherClient` (OpenWeather) |
| `HotelAgent` | destination, budget | `HotelResult(List<HotelOption>)` | `HotelMockRepository` |
| `BudgetAgent` | totalBudget, HotelResult, PriceResult | `BudgetBreakdown(hotel, food, transport, activities)` | none (pure computation) |
| `ItineraryAgent` | destination, days, activityBudget, interests, weatherWarnings | `List<ItineraryDay>` | `GeminiClient` |
| `RecommendationAgent` | all above results | `TripRecommendation(score, bestHotel, savings, verdict)` | none (pure computation) |
| `PlannerAgent` | `TripContext` | `TripResponse` | all seven agents above |

Each agent class stays under ~80 lines. The pattern for every "data-gathering" agent (Crowd, Price, Weather, Hotel) looks like this:

```java
@Component
public class CrowdAgent implements TravelAgent<TripContext, AgentResponse<CrowdResult>> {

    private final CrowdCalendarRepository repository;

    @Override
    public AgentResponse<CrowdResult> execute(TripContext ctx, TripContext context) {
        long start = System.currentTimeMillis();
        try {
            var record = repository.findByDestinationAndDate(ctx.destination(), ctx.travelDate());
            CrowdResult result = record != null
                ? new CrowdResult(record.getCrowdLevel(), record.getReason())
                : CrowdResult.defaultLow();
            return AgentResponse.ok(result, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return AgentResponse.failed(e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}
```

Note the `try/catch` inside every agent — this is deliberate (see §6, fault isolation).

---

## 4. Concurrency design — the most important LLD decision

The four data-gathering agents (Crowd, Price, Weather, Hotel) have **no dependency on each other** — they only depend on `TripContext`. Running them sequentially wastes latency for no reason. The Planner runs them in parallel using `CompletableFuture`, then joins before moving to the dependent stages.

```java
@Component
public class PlannerAgent {

    private final CrowdAgent crowdAgent;
    private final PriceAgent priceAgent;
    private final WeatherAgent weatherAgent;
    private final HotelAgent hotelAgent;
    private final BudgetAgent budgetAgent;
    private final ItineraryAgent itineraryAgent;
    private final RecommendationAgent recommendationAgent;
    private final Executor agentExecutor; // dedicated thread pool, not ForkJoinPool.commonPool()

    public TripResponse plan(TripContext ctx) {

        // Stage 1 — independent agents, fan out in parallel
        var crowdFuture   = CompletableFuture.supplyAsync(() -> crowdAgent.execute(ctx, ctx), agentExecutor);
        var priceFuture   = CompletableFuture.supplyAsync(() -> priceAgent.execute(ctx, ctx), agentExecutor);
        var weatherFuture = CompletableFuture.supplyAsync(() -> weatherAgent.execute(ctx, ctx), agentExecutor);
        var hotelFuture   = CompletableFuture.supplyAsync(() -> hotelAgent.execute(ctx, ctx), agentExecutor);

        CompletableFuture.allOf(crowdFuture, priceFuture, weatherFuture, hotelFuture).join();

        var crowdResult   = crowdFuture.join();
        var priceResult   = priceFuture.join();
        var weatherResult = weatherFuture.join();
        var hotelResult   = hotelFuture.join();

        // Stage 2 — depends on stage 1 results, sequential
        var budgetResult = budgetAgent.execute(
            new BudgetInput(ctx.totalBudget(), hotelResult.data(), priceResult.data()), ctx);

        // Stage 3 — depends on budget + weather, calls Gemini (I/O bound, slowest step)
        var itineraryResult = itineraryAgent.execute(
            new ItineraryInput(ctx, budgetResult.data(), weatherResult.data()), ctx);

        // Stage 4 — synthesis, depends on everything above
        var recommendation = recommendationAgent.execute(
            new RecommendationInput(crowdResult.data(), priceResult.data(),
                hotelResult.data(), budgetResult.data(), itineraryResult.data()), ctx);

        return TripResponse.builder()
            .tripId(ctx.tripId())
            .itinerary(itineraryResult.data())
            .budget(budgetResult.data())
            .recommendation(recommendation.data())
            .weatherWarnings(weatherResult.data().warnings())
            .build();
    }
}
```

**Why a dedicated `Executor` and not `ForkJoinPool.commonPool()`**: the common pool is shared JVM-wide; a slow Gemini call or a slow weather API blocking a common-pool thread can starve unrelated requests. Configure a bounded `ThreadPoolTaskExecutor` (e.g. core size 8, max 20, queue 100) specifically for agent execution.

**Timeout handling**: wrap each `CompletableFuture` with `.orTimeout(3, TimeUnit.SECONDS)` and a `.exceptionally()` fallback so one slow external API (e.g. Weather) doesn't hang the entire trip generation.

---

## 5. Design patterns used (and why)

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `TravelAgent<I,O>` interface | All agents are interchangeable strategies with the same contract — Planner doesn't care how each agent internally works |
| **Facade** | `PlannerAgent` | Hides the complexity of 7 agent calls behind one `plan()` method for `TripService` |
| **Builder** | `TripResponse.builder()` | Response has many optional/derived fields (weather warnings, recommendation, PDF link) — builder avoids a 10-argument constructor |
| **Template method** (optional) | `AbstractAgent` base class | If agents repeat the same try/catch/timing boilerplate, extract it into a base class with a `doExecute()` hook for subclasses |
| **Repository** | Spring Data JPA | Standard abstraction over PostgreSQL access |
| **DTO / Adapter** | `dto` package | Entities never leave the service layer directly — DTOs decouple the API contract from the DB schema |

---

## 6. Fault isolation and error handling

Agentic systems fail differently from normal CRUD apps — one bad external API call shouldn't kill the whole request. Rules:

1. **Every agent catches its own exceptions** and returns `AgentResponse.failed(...)` instead of throwing. The Planner never crashes because one agent had an issue.
2. **Every downstream agent has a fallback for a failed upstream result.** E.g. if `WeatherAgent` fails, `ItineraryAgent` proceeds without weather warnings rather than blocking the whole trip.
3. **Global exception handler** (`@RestControllerAdvice`) catches anything unexpected at the controller boundary and returns a clean `4xx/5xx` JSON error — never a raw stack trace to the client.
4. **Gemini-specific handling**: wrap the Gemini call with a retry (1 retry, exponential backoff) for transient errors, and a strict JSON-schema validation step after the response comes back — if Gemini returns malformed JSON, retry once with a stricter prompt before failing the whole trip generation.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TripGenerationException.class)
    public ResponseEntity<ErrorResponse> handle(TripGenerationException e) {
        return ResponseEntity.status(502).body(new ErrorResponse(e.getMessage(), e.getFailedAgent()));
    }
}
```

---

## 7. API contract (request/response)

**POST /api/trip/generate**

Request:
```json
{
  "destination": "Manali",
  "budget": 15000,
  "days": 3,
  "people": 2,
  "travelDate": "2026-08-15",
  "interests": ["nature", "food"]
}
```

Response:
```json
{
  "tripId": "b3f1...",
  "itinerary": [
    { "day": 1, "activities": [
      { "name": "Hadimba Temple", "startTime": "09:00", "endTime": "10:30", "estimatedCost": 0 },
      { "name": "Mall Road lunch", "startTime": "13:00", "endTime": "14:00", "estimatedCost": 600 }
    ]}
  ],
  "budget": { "hotel": 7200, "food": 3000, "transport": 2500, "activities": 2300 },
  "weatherWarnings": ["Rain expected on day 2 — indoor activities suggested"],
  "recommendation": {
    "tripScore": 78,
    "bestHotel": { "name": "Hotel Snow Valley", "price": 2400 },
    "crowdLevel": "HIGH",
    "bestTravelDate": "2026-08-22",
    "estimatedSavings": 4500,
    "verdict": "Consider shifting your trip by a week — you'd save about ₹4,500 and avoid peak-weekend crowds."
  }
}
```

**GET /api/should-i-travel?destination=Manali&budget=15000&date=2026-08-15**
— standalone version of the same Crowd + Price + Recommendation flow, without generating a full itinerary. Reuses `CrowdAgent`, `PriceAgent`, and a lightweight variant of `RecommendationAgent`.

---

## 8. Security design (JWT flow)

```
Login request → AuthController → AuthenticationManager
      → on success: JwtService.generateToken(userId)
      → returned to client, stored in memory (not localStorage, to avoid XSS token theft)

Every subsequent request → JwtFilter (OncePerRequestFilter)
      → extracts Bearer token → validates signature + expiry
      → sets Authentication in SecurityContext
      → request proceeds to controller
```

`SecurityConfig` whitelists `/api/auth/**` and locks everything else behind the filter. Passwords hashed with `BCryptPasswordEncoder`.

---

## 9. Testing strategy

| Layer | Test type | Example |
|---|---|---|
| Each agent | Unit test with mocked repository/client | `CrowdAgentTest`: assert `HIGH` crowd level returned for a long-weekend date |
| PlannerAgent | Unit test with all 7 agents mocked | Assert `plan()` still returns a valid response when `WeatherAgent` mock throws |
| GeminiClient | Unit test with a stubbed HTTP response | Assert malformed JSON triggers exactly one retry |
| Controller | `@WebMvcTest` with mocked service | Assert 400 on missing `destination` field |
| End-to-end | `@SpringBootTest` + Testcontainers (Postgres) | Full trip generation flow against a real DB, mocked Gemini |

---

## 10. What to actually build first (mapping back to the roadmap)

This LLD is heaviest in §4 (concurrency) and §6 (fault isolation) — those are the two things that make this a genuine "agentic system" rather than a sequence of function calls. When you reach Day 11–14 of the roadmap, build in this order:

1. `TravelAgent` interface + `TripContext` + `AgentResponse` (the contracts, ~30 min)
2. The four independent agents as simple synchronous classes first — get them working one at a time, sequentially, no `CompletableFuture` yet
3. `PlannerAgent` calling them sequentially — confirm the whole pipeline works end-to-end
4. **Only then** introduce `CompletableFuture` parallelism in the Planner — swapping sequential calls for parallel ones is a small, safe change once the sequential version already works, and it gives you a clean "before/after" story for the interview ("I built it sequentially first, measured the latency, then parallelized the independent agents and cut response time by X%")

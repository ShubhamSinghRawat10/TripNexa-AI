# SmartTrip AI — End-to-End Implementation Plan

**Multi-Agent, Budget & Crowd-Aware Travel Planner**
Stack: React + Tailwind → Spring Boot (Java 21) → Gemini API → PostgreSQL

---

## 1. Final Locked-In Concept

Combining everything from the earlier brainstorm into one final product:

> A user enters a destination, budget, dates, and interests. Instead of one AI prompt spitting out an itinerary, a set of specialized agents (Crowd, Price, Weather, Budget, Itinerary) each do one job, and a **Planner Agent** orchestrates them. The system doesn't just generate a plan — it tells the user whether *now* is even a good time to go, and how much they'd save by shifting their dates.

**Standout feature:** "Should I Travel Now?" — the thing that turns this from "AI itinerary generator #4000" into an actual agentic system with a real-world use case.

---

## 2. Tech Stack

| Layer | Choice |
|---|---|
| Frontend | React, Tailwind CSS, React Router, Axios |
| Backend | Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, Maven |
| Database | PostgreSQL (Redis optional, for caching price/crowd lookups) |
| AI | Gemini API (called directly from Spring Boot service layer — no need for a separate FastAPI service unless you want to demonstrate polyglot architecture) |
| External APIs | OpenWeather API, Google Maps API, Unsplash (images) |
| Price/Crowd data | Mocked/simulated dataset (see §6) — real-time hotel/flight price APIs are paid and not worth the cost for a resume project |

> Note: I dropped the separate FastAPI microservice from your earlier draft. For a fresher-level resume project, keeping the AI agents *inside* Spring Boot as a package (`agent/`) is easier to build, easier to debug, and still 100% legitimate to describe as an agentic architecture. Add FastAPI later only if you specifically want to show polyglot/microservice skills.

---

## 3. Multi-Agent Architecture

```
                        User Request
                             │
                        Planner Agent  (orchestrator)
                             │
        ┌──────────┬─────────┼─────────┬───────────┐
        ▼          ▼         ▼         ▼           ▼
   Crowd Agent  Price Agent  Weather Agent   Hotel Agent
        │          │         │         │
        └──────────┴────┬────┴─────────┘
                         ▼
                   Budget Agent (aggregates cost)
                         ▼
                  Itinerary Agent (calls Gemini)
                         ▼
              Recommendation Agent (final synthesis:
              best hotel, trip score, "should I travel now?")
                         ▼
                   Final JSON Response → React UI
```

Each agent implements the same interface, so it's trivial to explain and trivial to extend:

```java
public interface TravelAgent<I, O> {
    O execute(I input);
}
```

### Agent responsibilities

| Agent | Responsibility | Data source |
|---|---|---|
| **Planner Agent** | Orchestrates call order, merges results | — |
| **Crowd Agent** | Estimates crowd level for destination/date (weekend, long weekend, festival, school holidays) | Mock crowd dataset + Indian holiday calendar logic |
| **Price Agent** | Compares current hotel price vs. baseline price for that destination | Mock price dataset (baseline + surge multiplier on peak dates) |
| **Weather Agent** | Fetches forecast, flags days unsuitable for outdoor activities | OpenWeather API |
| **Hotel Agent** | Picks 2–3 hotel options within budget | Mock hotel dataset |
| **Budget Agent** | Splits total budget into hotel/food/transport/activities, checks feasibility | Derived from other agents |
| **Itinerary Agent** | Generates day-wise plan | Gemini API |
| **Recommendation Agent** | Combines everything → trip score, savings estimate, final verdict | Derived |

---

## 4. Database Schema

```sql
-- Users
users (
  id UUID PK,
  name VARCHAR,
  email VARCHAR UNIQUE,
  password VARCHAR,
  preferred_transport VARCHAR,
  budget_preference VARCHAR,
  created_at TIMESTAMP
)

-- Trips
trips (
  id UUID PK,
  user_id UUID FK -> users,
  destination VARCHAR,
  budget INT,
  days INT,
  people INT,
  travel_date DATE,
  interests VARCHAR[],   -- adventure, food, nightlife, etc.
  status VARCHAR,        -- DRAFT, GENERATED, SAVED
  created_at TIMESTAMP
)

-- Itinerary items
itinerary_items (
  id UUID PK,
  trip_id UUID FK -> trips,
  day INT,
  activity VARCHAR,
  start_time TIME,
  end_time TIME,
  estimated_cost INT
)

-- Recommendation snapshot (output of Recommendation Agent)
trip_recommendations (
  id UUID PK,
  trip_id UUID FK -> trips,
  best_hotel_name VARCHAR,
  best_hotel_price INT,
  weather_warning TEXT,
  trip_score INT,             -- 0-100
  best_travel_date DATE,
  estimated_savings INT,
  crowd_level VARCHAR         -- LOW, MEDIUM, HIGH
)

-- Mock reference data (you seed this yourself)
destination_price_baseline (
  destination VARCHAR,
  avg_hotel_price INT,
  peak_multiplier FLOAT
)

destination_crowd_calendar (
  destination VARCHAR,
  date DATE,
  crowd_level VARCHAR,
  reason VARCHAR    -- e.g. "Long weekend", "Diwali", "Peak season"
)
```

---

## 5. Backend Folder Structure

```
src/main/java/com/smarttrip/
 ├── controller/
 │    ├── AuthController.java
 │    ├── TripController.java
 │    └── RecommendationController.java
 ├── service/
 │    ├── TripService.java
 │    └── PdfExportService.java
 ├── agent/
 │    ├── TravelAgent.java          (interface)
 │    ├── PlannerAgent.java
 │    ├── CrowdAgent.java
 │    ├── PriceAgent.java
 │    ├── WeatherAgent.java
 │    ├── HotelAgent.java
 │    ├── BudgetAgent.java
 │    ├── ItineraryAgent.java
 │    └── RecommendationAgent.java
 ├── repository/
 ├── entity/
 ├── dto/
 │    ├── TripRequest.java
 │    ├── AgentResponse.java
 │    └── TripResponse.java
 ├── config/
 │    └── GeminiConfig.java
 └── security/
      ├── JwtFilter.java
      └── SecurityConfig.java
```

---

## 6. How to Fake "Real-Time" Price & Crowd Data (important)

You don't need a paid live hotel API. Build a small seeded table (`destination_price_baseline`, `destination_crowd_calendar`) with ~15-20 popular Indian destinations, and write rules like:

- If `travel_date` falls on a weekend or within 3 days of a public holiday → `crowd_level = HIGH`, `price = baseline * peak_multiplier (1.3–1.6x)`
- Otherwise → `crowd_level = LOW/MEDIUM`, `price = baseline`

This is completely honest to describe in an interview: *"Live hotel/flight pricing APIs are paid, so I simulated realistic price/crowd behavior using a rule-based model seeded with real average prices — the agent architecture itself is what's being demonstrated."* Interviewers respect that framing far more than a shaky live API integration that breaks during a demo.

---

## 7. Sample Gemini Prompt (Itinerary Agent)

```
System: You are a travel itinerary planning assistant.

Input:
- Destination: {destination}
- Days: {days}
- Budget remaining for activities: {activityBudget}
- Interests: {interests}
- Weather warnings: {weatherWarnings}

Task: Generate a day-wise itinerary. For each day, list 3-4 activities
with estimated time slots and approximate cost. Avoid outdoor activities
on days flagged with weather warnings. Respond ONLY in JSON matching
this schema: [{ "day": 1, "activities": [{ "name", "startTime",
"endTime", "estimatedCost" }] }]
```

---

## 8. API Endpoints

```
POST   /api/auth/signup
POST   /api/auth/login

POST   /api/trip/create              -> save trip draft
POST   /api/trip/generate            -> runs full agent pipeline
GET    /api/trip/history
GET    /api/trip/{id}
DELETE /api/trip/{id}
GET    /api/trip/{id}/pdf            -> export itinerary as PDF

GET    /api/should-i-travel?destination=Manali&budget=15000&date=2026-08-15
       -> standalone "Should I Travel Now?" tool, doesn't need a full trip
```

---

## 9. Frontend Pages & Flow

```
Home → Login/Register → Dashboard
                            │
              ┌─────────────┼───────────────┐
              ▼             ▼               ▼
        Generate Trip   Trip History   Should I Travel Now?
              │
        (multi-step form: destination, budget,
         days, people, month, interests)
              │
        "AI Agents Thinking..." loading state
        (nice UX touch: show agent names sequentially —
         "Checking crowd levels...", "Comparing hotel prices...",
         "Reading weather forecast...", "Building itinerary...")
              │
        Trip Result Page
        (itinerary, best hotel, trip score, weather warning,
         "Should I Travel Now?" savings card, Download PDF)
```

---

## 10. Build Roadmap (15–20 Days)

| Days | Milestone |
|---|---|
| 1–3 | Spring Boot + PostgreSQL + JWT auth + Trip CRUD (no AI yet) |
| 4–6 | React frontend: auth pages, dashboard, trip form, trip history UI |
| 7–8 | OpenWeather + Google Maps integration, seed mock price/crowd tables |
| 9–10 | Gemini integration — single-call itinerary generation working end-to-end |
| 11–14 | Break into full multi-agent pipeline (Planner + Crowd + Price + Weather + Hotel + Budget + Itinerary + Recommendation) |
| 15–17 | "Should I Travel Now?" feature, PDF export, polish loading UX |
| 18–20 | Testing, deployment (Render/Railway backend + Vercel/Netlify frontend), README, resume/interview prep |

Build in this order — each phase is independently demoable, so even if you run out of time at day 12, you already have a working (non-agentic) travel planner to show, and you're layering agentic complexity on top rather than betting everything on it working at the end.

---

## 11. Interview Explanation (refined, ~2 minutes)

> "SmartTrip AI is a multi-agent travel planning system. Instead of sending one big prompt to an LLM, I split the reasoning into specialized agents — a Crowd Agent and Price Agent that flag peak-season surges, a Weather Agent that adjusts activity suggestions, a Budget Agent that allocates cost across categories, and an Itinerary Agent that generates the day-wise plan via Gemini. A Planner Agent orchestrates the call order, and a Recommendation Agent synthesizes everything into a final trip score and a 'should you travel now or later' recommendation — including estimated savings if the user shifts their dates by a few days. Since live hotel/flight pricing APIs are paid, I simulated realistic price and crowd behavior with a rule-based model seeded on real average prices, so the focus stays on the agent architecture and orchestration logic rather than third-party API cost. This modular design made each piece independently testable and easy to extend."

---

## 12. Resume Bullet Points

- Built **SmartTrip AI**, a multi-agent travel planning system (React, Spring Boot, PostgreSQL, Gemini API) where specialized agents (crowd, price, weather, budget, itinerary) are orchestrated by a planner agent to generate budget-optimized itineraries.
- Designed a rule-based crowd/price simulation engine to model peak-season demand surges, powering a "Should I Travel Now?" recommendation that estimates cost savings from date shifts.
- Implemented JWT-based authentication, RESTful APIs, and a responsive React UI with Tailwind CSS.
- Integrated OpenWeather and Google Maps APIs to make itinerary generation weather- and location-aware.

---

## Open Decisions For You

1. **FastAPI microservice or not?** — Recommend skipping it (see §2) unless you specifically want the polyglot story for interviews.
2. **Redis caching** — nice-to-have for repeated destination lookups, not essential for MVP.
3. **PDF export library** — Apache PDFBox or iText for Spring Boot.

Ping me when you're ready to start on Day 1 (Spring Boot + JWT + PostgreSQL setup) and I'll walk through the actual entity classes, security config, and controller code.

# TripNexa / SmartTrip AI

Multi-agent, budget- and crowd-aware travel planning platform. The implementation follows
[`SmartTrip_AI_Implementation_Plan.md`](SmartTrip_AI_Implementation_Plan.md) and
[`SmartTrip_AI_LLD.md`](SmartTrip_AI_LLD.md).

## Current implementation

- Java 25 backend with Maven Wrapperr
- Flyway-managed PostgreSQL schema
- Stateless JWT signup/login with BCrypt password hashing
- Authenticated Trip create, history, read, update, and delete APIs
- Per-user ownership enforcement (another user's trip returns `404`)
- Structured validation and authentication error responses
- H2-backed integration tests for auth, validation, ownership, and CRUD

## Run locally

Prerequisites: Java 25+ and PostgreSQL 17, or Docker for the provided database service.

The API runs at `http://localhost:8080`; health is available at
`GET /actuator/health`. Configuration can be overridden with the variables documented in
`.env.example`. Never use the development JWT secret in deployment.

## Implemented API

| Method | Endpoint | Authentication |
|---|---|---|
| POST | `/api/auth/signup` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/trip/create` | Bearer token |
| GET | `/api/trip/history` | Bearer token |
| GET | `/api/trip/{id}` | Bearer token |
| PUT | `/api/trip/{id}` | Bearer token |
| DELETE | `/api/trip/{id}` | Bearer token |

Run the full backend test suite with:

```powershell
cd backend
.\mvnw.cmd test
```

## Next build slice

Add seeded destination price/crowd data and the core agent contracts (`TravelAgent`,
`TripContext`, `AgentResponse`), then implement the deterministic Crowd, Price, Hotel, and
Budget agents before connecting weather and Gemini.

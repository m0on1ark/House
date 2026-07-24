# House

A web app for neighbours who live in the same building. Residents find their house
by address, meet each other, chat, and organise **group purchases** — splitting bulk
prices and delivery across several apartments.

> **Live demo:** https://house-x2s9.onrender.com
> *(Hosted on Render's free tier — the first request after idle may take ~30s to wake up.)*

This is a portfolio project and is **still in active development**.

## Features

- **Registration & authentication** — user accounts with password hashing.
- **Profiles** — avatar, floor, apartment, and a public profile page.
- **Building linking** — search for a building by address via OpenStreetMap /
  Nominatim and attach the user to a specific house (with coordinates and a map for
  picking the search area).
- **Neighbours directory** — browse residents of your building and their public profiles.
- **Group deals** — any resident posts a bulk purchase (product, store, total units,
  their own share, price per unit, photo); neighbours claim their part of it.
- **Chats** — a shared building chat and per-deal chats, with image messages.
- **UI** — animated landing page, collapsible navigation, and light / night themes.

## Tech stack

- **Java 21**
- **Spring Boot 4.1**
  - **Spring Web MVC** — controllers and request handling
  - **Spring Data JPA** — persistence layer
  - **Spring Security Crypto** — password hashing
- **Thymeleaf** — server-side HTML templating
- **PostgreSQL** — relational database
- **Flyway** — versioned database migrations
- **Maven** — build tool (`mvnw` wrapper included)
- **Docker / Docker Compose** — containerised local run
- **Nominatim (OpenStreetMap)** — address search
- **Render + Neon** — deployment (Docker app + managed Postgres)

## Architecture

The Java backend follows a standard layered Spring Boot structure:

```
src/main/java/House/prototype/
├── PrototypeApplication.java   — Spring Boot entry point
├── Controller/                 — HTTP endpoints (MVC controllers)
│   ├── UserController          — register, login, account, settings, residents
│   ├── BuildingController      — address search & building linking
│   ├── DealController          — create / claim / cancel / delete group deals
│   └── ChatController          — building chat and deal chats
├── Service/                    — business logic
│   ├── UserService, BuildingService, DealService, ChatService
├── Repository/                 — Spring Data JPA repositories
│   ├── UserRepository, BuildingRepository, DealRepository,
│   │   DealClaimRepository, DealMessageRepository, BuildingMessageRepository
├── Model/                      — JPA entities
│   ├── User, Building, Deal, DealClaim, DealStatus,
│   │   DealMessage, BuildingMessage
├── DTO/                        — data transfer objects (e.g. BuildingSuggestion)
└── External/                   — Nominatim integration (NominatimResponse)

src/main/resources/
├── db/migration/               — Flyway migrations (V1…V10)
├── templates/                  — Thymeleaf templates & fragments
├── static/                     — CSS and JavaScript
└── application.properties      — configuration
```

**Request flow:** `Controller` → `Service` → `Repository` → PostgreSQL, with
Thymeleaf rendering the response views. The database schema is managed entirely
through Flyway migrations, and JPA runs in `validate` mode against it.

## Running locally

### With Docker Compose (easiest)

Starts the app together with a PostgreSQL database:

```bash
docker compose up --build
```

The app is then available at http://localhost:8080.

### Manually

Requires **JDK 21** and a running **PostgreSQL**. Create the database and user (see
`compose.yaml`), or point the app at your own database via environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/house
export SPRING_DATASOURCE_USERNAME=house_user
export SPRING_DATASOURCE_PASSWORD=<password>

./mvnw spring-boot:run
```

Flyway applies the migrations on startup. The server listens on port `8080` by
default (override with the `PORT` environment variable).

## Deployment

`render.yaml` deploys the app to [Render](https://render.com) as a Docker service,
backed by a managed PostgreSQL database on [Neon](https://neon.tech). Database
credentials are supplied through the `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` environment variables.

## Status

Work in progress — a portfolio project under active development.

# House

[![CI](https://github.com/m0on1ark/House/actions/workflows/ci.yml/badge.svg)](https://github.com/m0on1ark/House/actions/workflows/ci.yml)

A web app for neighbours who live in the same building. Residents find their house
by address, meet each other, chat, and organise **group purchases** — splitting bulk
prices and delivery across several apartments.

> **Live demo:** https://house-x2s9.onrender.com
> *(Hosted on Render's free tier — the first request after idle may take ~30s to wake up.)*

This is a portfolio project and is **still in active development**.

## Features

- **Registration & authentication** — Spring Security form login over BCrypt-hashed
  passwords, with CSRF-protected forms and session fixation protection.
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
  - **Spring Security** — form login, URL authorization, CSRF, BCrypt hashing
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
├── External/                   — Nominatim integration (NominatimResponse)
└── Security/                   — Spring Security setup
    ├── SecurityConfig          — filter chain, URL rules, PasswordEncoder
    ├── AppUserDetailsService   — loads an account for form login
    ├── AppUserDetails          — the principal kept in the session (id + username)
    ├── AutoLogin               — signs a user in after registration
    └── CurrentUserAdvice       — exposes the principal to every view

src/main/resources/
├── db/migration/               — Flyway migrations (V1…V10)
├── templates/                  — Thymeleaf templates & fragments
├── static/                     — CSS and JavaScript
└── application.properties      — configuration
```

**Request flow:** `Controller` → `Service` → `Repository` → PostgreSQL, with
Thymeleaf rendering the response views. The database schema is managed entirely
through Flyway migrations, and JPA runs in `validate` mode against it.

**Error handling:** services throw `InvalidRequestException` for anything the
visitor caused — bad input, an action they are not allowed to take, an id in the
URL that does not exist. Controllers do not catch it one call at a time; an
`@ExceptionHandler` per controller turns it into a redirect back to the page the
visitor came from, with `?error`. Genuine "this should not happen" states (the
logged-in account's own row missing) stay `IllegalArgumentException` and are
deliberately left unhandled, so they surface as a server error instead of
redirecting to a page that would throw the same thing again. The few endpoints
whose failure is not a redirect — image responses, registration — still catch
locally, because their destination differs from the rest of the controller.

## Security

The filter chain leaves the landing page, login, registration and static assets
open and requires authentication for everything else, so a controller never has
to check for a logged-in user itself. Controllers receive the current account
through `@AuthenticationPrincipal`, views through the `currentUser` model
attribute. Login and logout are handled by Spring Security's filters — the only
`/login` method in `UserController` is the `GET` that renders the form.

Four decisions in there are worth explaining, because the alternative looks more
obvious than it is:

**The `PasswordEncoder` bean is a plain `BCryptPasswordEncoder`, not
`PasswordEncoderFactories.createDelegatingPasswordEncoder()`.** The delegating
encoder is the usual recommendation — it reads a `{bcrypt}` prefix off the hash
so the algorithm can be swapped later. But the hashes already in the database
were written by a bare `BCryptPasswordEncoder` and carry no prefix, so a
delegating encoder would reject every existing account on login. Switching to it
later means re-prefixing the stored hashes in a migration.

**`AppUserDetails` holds an id and a username rather than the `User` entity.**
The principal is stored in the HTTP session, and a JPA entity kept there goes
stale against the database and drags its lazy associations (`building`) along
with it. Controllers take the id from the principal and load fresh state
themselves.

**`AutoLogin` writes to the `SecurityContextRepository`, not just to the
`SecurityContextHolder`.** The holder is a `ThreadLocal` that lives for one
request, so setting it alone would leave the user logged out again after the
redirect that follows registration. Saving to the repository is what puts the
authentication into the session.

**Every state-changing form uses `th:action`, never a plain `action`.** CSRF
protection is on, and Thymeleaf only injects the hidden `_csrf` field for
`th:action`. A new POST form written with a plain `action` will fail with `403`.

Session fixation protection, the security response headers and logout cleanup
are Spring Security defaults and are not configured explicitly.

**Known gap:** changing a password does not invalidate that account's other
sessions — a stolen session survives it. That needs a `SessionRegistry`.

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

## Tests

```bash
./mvnw test
```

- **`SecurityRulesTest`** — a `@WebMvcTest` slice that drives the real security
  filter chain with `MockMvc`: an anonymous visitor is redirected to `/login`,
  a signed-in one reaches the same URL, a form POST without a CSRF token is
  rejected with `403`, and login succeeds or fails on the right credentials.
- **`DealControllerTest`** — error handling: a rejected claim redirects back with
  `?error`, a missing deal image is a `404` rather than a redirect, and a broken
  account does not turn the deals page into a redirect loop.
- **`UserServiceTest`** — plain Mockito unit tests for the account rules
  underneath: registration stores a hash rather than the password, a taken
  username is refused, a wrong current password blocks a password change, and a
  profile stays hidden from residents of another building.
- **`PrototypeApplicationTests`** — context load check; needs a reachable
  PostgreSQL, so run `docker compose up db` first if the app is not running.

GitHub Actions runs `./mvnw verify` on every push to `main` and on every pull
request (`.github/workflows/ci.yml`), against a throwaway PostgreSQL 16 service
container. Because that database starts empty, the run also proves the Flyway
migrations build the schema from nothing — not just that they match a database
that already exists.

## Deployment

`render.yaml` deploys the app to [Render](https://render.com) as a Docker service,
backed by a managed PostgreSQL database on [Neon](https://neon.tech). Database
credentials are supplied through the `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` environment variables.

## Roadmap

- **Live chat** — messages currently appear only after a page refresh.
- **Password reset** — no way back in once a password is forgotten.
- **Wider test coverage** — the deal and chat services are not covered yet.

## Status

Work in progress — a portfolio project under active development.

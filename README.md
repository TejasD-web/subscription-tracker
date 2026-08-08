# Subscription Tracker

A locally-run subscription management system built across three languages: a Java/Spring Boot REST API, a Rust CLI companion tool, and (planned) a React frontend — with an experimental Plaid integration for automatic bank-based subscription detection.

Built as a portfolio project to demonstrate backend engineering fundamentals: REST API design, JPA/Hibernate, scheduled tasks, email integration, comprehensive testing, and cross-language interoperability.

---

## Features

- **Full CRUD** for tracking subscriptions — cost, billing cycle, category, renewal dates
- **Spending analytics** — monthly/yearly totals normalized across weekly/monthly/yearly billing cycles, spend-by-category breakdown, cost-impact previews for adding/removing subscriptions
- **Email reminders** — automated renewal reminders and trial-expiry prompts via Gmail SMTP, with clickable confirm/cancel links
- **Trial & try-out tracking** — dedicated subscription states (`TRIAL`, `TRYING_OUT`) with an end-to-end email confirmation flow, not just a boolean flag
- **Daily scheduler** — `@Scheduled` job checks for upcoming renewals and expiring trials
- **Rust CLI** — a companion terminal tool that consumes the same REST API, with an interactive menu and direct commands for listing, adding, and managing subscriptions
- **Experimental Plaid integration** *(stretch goal)* — on-demand bank-transaction scanning to auto-detect recurring charges, using a manual-confirmation `PENDING_REVIEW` state to guard against false positives (tested against real Plaid Sandbox data — see notes below)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Data JPA, Hibernate |
| Database | H2 (file-mode, persists locally) |
| Testing | JUnit 5, Mockito, MockMvc (23 tests) |
| Email | Jakarta Mail / Gmail SMTP |
| CLI | Rust, `reqwest`, `serde`, `chrono` |
| Bank integration | Plaid API (Sandbox) |
| Build | Maven (backend), Cargo (CLI) |

---

## Project Structure

```
subscription-tracker/
├── backend/    # Spring Boot REST API
│   └── src/main/java/com/subscriptions/api/
│       ├── controller/   # REST endpoints
│       ├── service/      # Business logic
│       ├── repository/   # Spring Data JPA repositories
│       ├── model/        # JPA entities and enums
│       ├── dto/          # Request/response DTOs
│       ├── scheduler/    # Daily @Scheduled job
│       ├── plaid/        # Plaid integration module
│       └── exception/    # Centralized error handling
├── cli/        # Rust companion CLI
└── frontend/   # React frontend (planned, not yet built)
```

---

## Running Locally

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```
API available at `http://localhost:8080`. H2 console at `http://localhost:8080/h2-console`.

**CLI:**
```bash
cd cli
cargo build
./target/debug/subcli          # interactive menu
./target/debug/subcli list     # or direct commands
```

### Configuration

Email (Gmail SMTP) and Plaid credentials are read from environment variables — never committed to the repo:
```bash
export GMAIL_USERNAME="your-email@gmail.com"
export GMAIL_APP_PASSWORD="your-app-password"
export PLAID_CLIENT_ID="your-plaid-client-id"
export PLAID_SECRET="your-plaid-secret"
```
The app runs and is fully functional with none of these set — email sending and Plaid endpoints simply no-op with a logged warning if unconfigured.

---

## Design Notes

- **DTOs are kept separate from JPA entities** so the API contract isn't tied directly to the database schema.
- **`ddl-auto=update`** is used for local development convenience; a production deployment would use a proper migration tool (Flyway/Liquibase) instead, since `update` doesn't handle all schema changes (e.g. widening an enum constraint on an existing column).
- **Cancellation is assisted, not automated** — the app stores a cancellation URL per subscription and surfaces it to the user rather than attempting to auto-cancel on external sites.
- **Plaid detection is on-demand, not scheduled**, and deliberately uses Plaid's own recurring-transaction detection rather than custom pattern matching — this keeps the experimental, third-party-dependent code isolated from the reliable core reminder system.
- **`PENDING_REVIEW` exists specifically to prevent false positives.** Testing against Plaid's Sandbox data surfaced exactly this risk in practice — a credit card autopay and a one-off airline charge were flagged as "recurring" alongside genuine subscription-like charges. Requiring manual confirmation avoids auto-creating incorrect subscriptions.
- **Known limitation:** the daily reminder scheduler only runs while the Spring Boot process is active, since it's an in-process `@Scheduled` job. This is a deliberate tradeoff for a local-first tool with no deployment requirement; moving to an always-on host (Docker + a small cloud VM, with H2 swapped for Postgres) would resolve it without any change to the scheduler code itself.

---

## Status

| Phase | Status |
|---|---|
| Java backend | ✅ Complete — 23 passing tests |
| Rust CLI | ✅ Complete |
| Plaid integration (stretch) | ✅ Complete, verified against Sandbox data |
| React frontend | ⏳ Not yet started |

---

## Author

Tejas Krishna Datla — CS student, University of Maryland

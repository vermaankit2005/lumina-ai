# Lumina AI — Personal Email Briefing Agent

> **Self-hosted. No SaaS trust required. Your credentials never leave your machine.**

Lumina connects to your Gmail, passes unread emails through an LLM, extracts action items, and delivers a structured briefing to your Telegram every morning. Three scheduled jobs run the full loop — you open Telegram, see what matters, reply `done #N` to close tasks.

---

## Demo

> **Add a screenshot here** — paste a real Telegram briefing message and the evening digest side by side. A single image does more than any paragraph.

**Telegram commands supported:**

| Command | What it does |
|---|---|
| `/briefing` | Trigger a briefing on-demand |
| `/tasks` | List all open tasks |
| `done #N` | Mark task N as complete |

---

## How It Works

```
07:00 ── DailyBriefingJob ──▶ Gmail (last 24 h)
                               │
                          Deduplicate
                          (ProcessedEmail)
                               │
                           LLM Analysis
                           (Ollama / Groq)
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
             ActionTask             Telegram Briefing
            (PostgreSQL)
                    │
08:00 ── DeadlineAlertJob ──▶ Telegram (tasks due ≤ 2 days)
18:00 ── TaskDigestJob    ──▶ Telegram (open tasks, urgency-sorted)
```

The three jobs are independent and idempotent — restarting the application never sends a duplicate message.

---

## Features

- **Morning briefing (7 AM)** — fetches Gmail, deduplicates, sends structured summary + action items to Telegram
- **Deadline alerts (8 AM)** — pushes a reminder for any open task due within 2 days; fires once per task per day
- **Evening digest (6 PM)** — lists all open tasks sorted by urgency score, colour-coded 🔴🟡🟢
- **Telegram command interface** — `/briefing`, `/tasks`, `done #N` handled in real time
- **Idempotent pipeline** — emails marked processed only after successful Telegram send; failed runs retry cleanly next cycle
- **Swappable adapters** — Gmail ↔ Mailpit, Ollama ↔ Groq — switch profiles, not code
- **Full audit trail** — every `BriefingRun` persisted with status, email count, task count, and error message if failed

---

## Architecture

The application is a **modular monolith** built around **Ports & Adapters** (Hexagonal Architecture). The briefing pipeline depends only on interfaces. Infrastructure adapters are injected via Spring and swapped by profile.

```
┌─────────────────────────────────────────────────────────────┐
│                        Schedulers                            │
│  DailyBriefingJob (7AM) · DeadlineAlertJob (8AM)            │
│  TaskDigestJob (6PM)                                         │
└──────────────────┬──────────────────────┬───────────────────┘
                   │                      │
┌──────────────────▼──────────────────────▼───────────────────┐
│                    DailyBriefingService                      │
│                                                              │
│  EmailFetcherPort ──▶ EmailAnalysisPort ──▶ NotificationPort │
│        │                    │                    │           │
│  GmailFetchService     LLMService          TelegramNotif.   │
│  MailPitFetchService   (Ollama/Groq)       Service          │
│  (test profile)                                              │
└──────────────────────────┬──────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
       PostgreSQL                  Telegram Bot API
    (JPA · Flyway)             (outbound briefings +
  ActionTask                    inbound commands)
  BriefingRun
  ProcessedEmail
```

**Inbound command path:**

```
Telegram message ──▶ LuminaTelegramBot ──▶ CommandParser ──▶ TelegramCommandHandler
                                                                       │
                                                               ActionTaskRepository
```

---

## Design Decisions & Trade-offs

These are the choices worth discussing — each had a real alternative.

### Modular Monolith over Microservices

This is a single-user, single-process tool. Microservices would add service discovery, network serialisation, distributed tracing, and container orchestration — all overhead with zero user benefit. A modular monolith gives clean package separation without ops complexity. If the tool grew multi-tenant, the service layer is already the seam to split on.

### Ports & Adapters for Infrastructure Isolation

`EmailFetcherPort`, `EmailAnalysisPort`, and `NotificationPort` are the only interfaces the briefing pipeline sees. `DailyBriefingService` imports zero Gmail, Ollama, or Telegram classes. This made the local test profile trivial: swap `GmailFetchService` for `MailPitFetchService` and `LLMService` for a Groq-compatible endpoint by changing Spring bean registration, not business logic. It also makes the service layer fully unit-testable with simple mocks.

### Two-Layer Idempotency

Re-running the pipeline must never send duplicate messages. Two independent guards:

1. **`ProcessedEmail` table** — stores Gmail message IDs. Any email seen in a previous run is filtered before LLM analysis. This is per-email, permanent.
2. **`BriefingRun` status check** — `DailyBriefingJob` queries for a `SUCCESS` run for today before executing. A second trigger (manual or scheduler) is a no-op.

Emails are marked processed **only after** the Telegram send succeeds. A network failure at notification time means the next run will re-analyse and re-send — no silent data loss.

### Numeric Urgency Score over LLM Priority

The LLM extracts a `priority` field (HIGH / MEDIUM / LOW). That is kept on the entity, but the evening digest sorts by `TaskUrgencyScorer`, which computes a numeric score from deadline proximity and task age:

- Overdue → 1000 + (days overdue × 10)
- Due today → 900
- Due in ≤ 2 days → 800
- Due in ≤ 7 days → 500
- No deadline → age × 5 (capped at 400)

LLM priority is categorical and inconsistent across emails. The scorer is deterministic, deadline-aware, and testable without an LLM.

### Deadline Alerts: `reminderSentDate` per Task

`DeadlineAlertJob` runs daily. To prevent re-alerting the same task every morning, each `ActionTask` stores a `reminderSentDate`. The job filters out any task where `reminderSentDate == today`. This is a single column instead of a separate join table — sufficient for a one-alert-per-day-per-task constraint.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| AI | Spring AI · Ollama (production) · OpenAI-compatible endpoint (test profile) |
| Email | Gmail API v1 (OAuth 2.0) · Mailpit (local SMTP stub) |
| Messaging | Telegram Bot API (LongPolling) |
| Database | PostgreSQL 16 · Spring Data JPA · Flyway |
| Build | Gradle 8 (Kotlin DSL) |

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- [Ollama](https://ollama.com/) with `mistral` pulled — or a Groq API key for the test profile
- Gmail OAuth credentials (one-time setup, see below)
- Telegram bot token and personal chat ID (one-time setup, see below)

### 1. Clone and configure

```bash
git clone https://github.com/vermaankit2005/lumina-ai.git
cd lumina-ai
cp .env.example .env
# Fill in POSTGRES_PASSWORD, TELEGRAM_BOT_TOKEN, TELEGRAM_ALLOWED_CHAT_ID
```

### 2. Start infrastructure

```bash
docker compose up -d
# PostgreSQL on :5432 · Mailpit on :8025
```

### 3. Pull the LLM model

```bash
ollama pull mistral
```

### 4. Run

```bash
./gradlew bootRun
```

The application starts, registers the scheduled jobs, and begins listening for Telegram commands. The first briefing fires at 7 AM, or trigger it immediately with `/briefing`.

---

## Gmail Integration (one-time setup)

Lumina uses OAuth 2.0 with read-only Gmail scope. Tokens are stored locally and reused automatically.

1. Create a Google Cloud project at [console.cloud.google.com](https://console.cloud.google.com)
2. Enable the Gmail API: *APIs & Services → Enable APIs → Gmail API → Enable*
3. Create credentials: *Credentials → Create Credentials → OAuth client ID → Desktop application*
4. Download `credentials.json` and place it anywhere on your machine
5. Set `GMAIL_CREDENTIALS_FILE_PATH` in `.env` to that path
6. On the first run, a browser window opens for authorisation. A `tokens/` directory is created beside the credentials file and reused on all subsequent runs.

---

## Telegram Integration (one-time setup)

1. Start a chat with [@BotFather](https://t.me/BotFather) on Telegram
2. Send `/newbot` and follow the prompts
3. Copy the bot token (`123456789:ABCdef...`)
4. Send any message to your new bot, then open `https://api.telegram.org/bot<TOKEN>/getUpdates`
5. Find `"chat": { "id": ... }` in the response — that is your chat ID

---

## Local Development (Test Profile)

Replaces Gmail with Mailpit and uses Groq instead of a local Ollama install.

```bash
docker compose up mailpit -d
bash docs/send-test-emails.sh
SPRING_PROFILES_ACTIVE=test GROQ_API_KEY=<your-key> ./gradlew bootRun
```

Mailpit web UI: [http://localhost:8025](http://localhost:8025)

---

## Configuration Reference

| Variable | Default | Required |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | |
| `POSTGRES_DB` | `luminaai` | |
| `POSTGRES_USER` | `lumina` | |
| `POSTGRES_PASSWORD` | — | Yes |
| `GMAIL_CREDENTIALS_FILE_PATH` | `credentials.json` | |
| `TELEGRAM_BOT_TOKEN` | — | Yes |
| `TELEGRAM_BOT_USERNAME` | `lumina_my_bot` | |
| `TELEGRAM_ALLOWED_CHAT_ID` | — | Yes |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | |
| `OLLAMA_MODEL` | `mistral` | |
| `GROQ_API_KEY` | `not-used` | Test profile only |

---

## Running Tests

```bash
./gradlew test
```

Tests use H2 in-memory database and the `test` Spring profile. No external services required.

### Local test configuration (required)

`src/test/resources/application-test.yml` is gitignored. Create it before running tests:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false

lumina:
  telegram:
    bot-token: test-bot-token
    allowed-chat-id: 0
```

---

## Project Structure

```
src/main/java/com/luminaai/
├── config/            # Gmail OAuth, LLM client, Telegram configuration
├── domain/
│   ├── enums/         # RunStatus, TaskPriority, TaskStatus
│   ├── exception/     # LuminaException hierarchy
│   └── model/         # EmailMessage, AnalysisResult (domain DTOs)
├── entity/            # JPA entities: ActionTask, BriefingRun, ProcessedEmail
├── port/              # Interfaces: EmailFetcherPort, NotificationPort, EmailAnalysisPort
├── repository/        # Spring Data JPA repositories
├── runner/            # DailyBriefingRunner (CommandLineRunner entry point)
├── scheduler/         # DailyBriefingJob, DeadlineAlertJob, TaskDigestJob
├── service/
│   ├── ai/            # LLMService — EmailAnalysisPort implementation
│   ├── briefing/      # DailyBriefingService, BriefingFormatter
│   ├── gmail/         # GmailFetchService, MailPitFetchService
│   ├── notification/  # TelegramNotificationService
│   └── task/          # TaskUrgencyScorer
└── telegram/          # LuminaTelegramBot, CommandParser, TelegramCommandHandler

src/main/resources/
├── db/migration/      # Flyway SQL migrations (V1__..., V2__..., etc.)
└── prompts/           # LLM prompt templates
```

---

## Roadmap

| Sprint | Focus | Status |
|---|---|---|
| Sprint 1 | Gmail OAuth2 integration | ✅ Done |
| Sprint 2 | Telegram bot + echo | ✅ Done |
| Sprint 3 | PostgreSQL persistence + Flyway | ✅ Done |
| Sprint 4 | LLM integration (email analysis, task extraction) | ✅ Done |
| Sprint 5 | Test quality + architecture polish (Ports & Adapters) | ✅ Done |
| Sprint 6 | Scheduled jobs + Telegram commands (`/briefing`, `/tasks`, `done #N`) | ✅ Done |
| Sprint 7 | Proactive task reminders (evening digest, deadline alerts, urgency scoring) | ✅ Done |
| Sprint 8 | Production hardening (retry, startup validation, encrypted token storage) | Planned |
| Sprint 9 | REST API + OpenAPI/Swagger | Planned |

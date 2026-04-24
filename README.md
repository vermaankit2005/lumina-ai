# Lumina AI — Personal Productivity Agent

Lumina AI is a Spring Boot application that automates daily email triage. It fetches unread emails from the last 24 hours, sends them through an LLM for analysis, extracts structured action items, persists them to PostgreSQL, and delivers a formatted briefing to Telegram.

The motivation was straightforward: I wanted a way to get a clear, prioritised summary of my inbox every morning without opening a browser. A single Telegram message is enough.

```
Gmail ──▶ LLM Analysis ──▶ Action Tasks (PostgreSQL)
                    │
                    └──▶ Telegram Briefing
```

This is also a deliberate learning project. Each sprint introduces one real capability and one architectural pattern — the goal being something I use daily and something I can walk through in depth technically.

---

## Features

- Fetches emails from Gmail via OAuth 2.0 (read-only scope)
- Analyses email content with a locally-running LLM (Ollama/Mistral) or a hosted endpoint (Groq/OpenAI-compatible)
- Extracts an executive summary and a prioritised list of action items per email
- Persists every briefing run and extracted task to PostgreSQL for history and querying
- Deduplicates across runs — re-running never double-counts processed emails
- Delivers the briefing as a formatted Markdown message to Telegram
- Supports a local test profile using Mailpit (SMTP stub) — no real credentials needed during development

---

## Architecture

The application is structured as a **modular monolith** using a **Ports & Adapters** layout. The core briefing pipeline operates against interfaces only; infrastructure adapters (Gmail, Mailpit, Ollama, Telegram) are injected and swappable.

```
┌─────────────────────────────────────────────────────────┐
│                    DailyBriefingRunner                  │  ← Spring CommandLineRunner
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  DailyBriefingService                    │
│                                                          │
│  ┌──────────────────┐  ┌───────────────────┐            │
│  │ EmailFetcherPort │  │ EmailAnalysisPort  │            │
│  └───────┬──────────┘  └─────────┬─────────┘            │
│          │                       │                       │
│  ┌───────▼──────┐     ┌──────────▼────────┐             │
│  │GmailFetch    │     │    LLMService      │             │
│  │Service       │     │ (Ollama / Groq)    │             │
│  │MailPitFetch  │     └────────────────────┘             │
│  │Service (test)│                                        │
│  └──────────────┘     ┌──────────────────┐              │
│                        │ NotificationPort │              │
│                        └────────┬─────────┘              │
│                                 │                         │
│                        ┌────────▼──────────┐             │
│                        │TelegramNotification│             │
│                        │Service             │             │
│                        └────────────────────┘             │
└─────────────────────────────────────────────────────────┘
         │                            │
  ┌──────▼──────┐            ┌────────▼──────┐
  │ PostgreSQL  │            │   Telegram    │
  │ (JPA/Flyway)│            │   Bot API     │
  └─────────────┘            └───────────────┘
```

**Design decisions:**

| Pattern | Where used |
|---|---|
| Ports & Adapters | `EmailFetcherPort`, `NotificationPort`, `EmailAnalysisPort` decouple business logic from infrastructure |
| Strategy | Gmail vs. Mailpit adapters swapped via Spring `@Profile` |
| Repository | Spring Data JPA repositories encapsulate all database access |
| Builder | Lombok `@Builder` on all JPA entities for safe, readable construction |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| AI | Spring AI · Ollama (production) · OpenAI-compatible endpoint (test profile) |
| Email | Gmail API v1 (OAuth 2.0) · Mailpit (local SMTP stub) |
| Messaging | Telegram Bot API |
| Database | PostgreSQL 16 · Spring Data JPA · Flyway |
| Build | Gradle 8 (Kotlin DSL) |

---

## Prerequisites

- Java 21+
- Docker & Docker Compose (for PostgreSQL and Mailpit)
- [Ollama](https://ollama.com/) with the `mistral` model pulled — or a Groq/OpenAI API key for the test profile
- A Gmail account with a Google Cloud project configured for OAuth 2.0
- A Telegram bot token and your personal chat ID

---

## Getting Started

### 1. Clone and configure

```bash
git clone https://github.com/vermaankit2005/lumina-ai.git
cd lumina-ai
cp .env.example .env
# Fill in the required values — see the Configuration section below
```

### 2. Start infrastructure

```bash
docker compose up -d
# Starts PostgreSQL on port 5432 and Mailpit on port 8025
```

### 3. Pull the LLM model

```bash
ollama pull mistral
```

### 4. Run the application

```bash
./gradlew bootRun
```

The application starts, executes the briefing pipeline, delivers the Telegram message, and exits.

---

## Gmail Integration (one-time setup)

Lumina uses OAuth 2.0 with read-only Gmail scope. This is configured once and tokens are stored locally for subsequent runs.

1. Create a Google Cloud project at [console.cloud.google.com](https://console.cloud.google.com)
2. Enable the Gmail API: *APIs & Services → Enable APIs → Gmail API → Enable*
3. Create credentials: *Credentials → Create Credentials → OAuth client ID → Desktop application*
4. Download the `credentials.json` file and place it somewhere on your machine
5. Set `GMAIL_CREDENTIALS_FILE_PATH` in `.env` to that path
6. On the first run, a browser window opens for authorisation. After approval, a `tokens/` directory is created next to the credentials file and reused automatically on subsequent runs.

---

## Telegram Integration (one-time setup)

1. Open Telegram and start a chat with [@BotFather](https://t.me/BotFather)
2. Send `/newbot` and follow the prompts to create a bot
3. Copy the bot token provided at the end (`123456789:ABCdef...`)
4. Send any message to your new bot, then open `https://api.telegram.org/bot<TOKEN>/getUpdates`
5. Find `"chat": { "id": ... }` in the JSON response — that is your chat ID

---

## Local Development (Test Profile)

The test profile replaces Gmail with Mailpit and uses a Groq/OpenAI-compatible endpoint instead of a local Ollama install.

```bash
# Start Mailpit
docker compose up mailpit -d

# Seed with test emails
bash docs/send-test-emails.sh

# Run with the test profile
SPRING_PROFILES_ACTIVE=test GROQ_API_KEY=<your-key> ./gradlew bootRun
```

Mailpit's web UI is available at [http://localhost:8025](http://localhost:8025).

---

## Configuration Reference

Copy `.env.example` to `.env` and set the required values.

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

Tests use an H2 in-memory database and the `test` Spring profile. No external services are required.

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
├── service/
│   ├── ai/            # LLMService — EmailAnalysisPort implementation
│   ├── briefing/      # DailyBriefingService, BriefingFormatter
│   ├── gmail/         # GmailFetchService, MailPitFetchService
│   └── notification/  # TelegramNotificationService
└── telegram/          # LuminaTelegramBot (inbound command handling)

src/main/resources/
├── db/migration/      # Flyway SQL migrations
└── prompts/           # LLM prompt templates
```

---

## Roadmap

The project is built incrementally in sprints, each targeting a specific capability:

| Sprint | Focus | Status |
|---|---|---|
| Sprint 1 | Gmail OAuth2 integration | ✅ Done |
| Sprint 2 | Telegram bot + echo | ✅ Done |
| Sprint 3 | PostgreSQL persistence + Flyway | ✅ Done |
| Sprint 4 | LLM integration | ⚠️ Partial — batch email processing pending |
| Sprint 5 | Test quality + architecture polish | Planned |
| Sprint 6 | Scheduled job + Telegram commands (`/briefing`, `/tasks`, `done #N`) | Planned |
| Sprint 7 | Production hardening (retry, startup validation, encrypted token storage) | Planned |
| Sprint 8 | REST API + OpenAPI/Swagger | Planned |

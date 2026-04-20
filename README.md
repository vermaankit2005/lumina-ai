# Lumina AI — Personal Email Briefing Agent

Lumina AI is a Spring Boot application that runs once a day to give you an AI-powered briefing of your inbox. It fetches emails from the last 24 hours, sends them to a local LLM (Ollama / Mistral), extracts structured action items, persists them to PostgreSQL, and delivers a formatted summary to your Telegram chat.

```
Gmail ──▶ LLM Analysis ──▶ Action Tasks (PostgreSQL)
                     │
                     └──▶ Telegram Briefing
```

---

## Features

- **AI-powered summarisation** — prompts a locally-running LLM to produce an executive summary and a prioritised task list from raw email threads
- **Deduplication** — tracks processed email IDs so re-runs never double-count messages
- **Persistent audit log** — every briefing run and extracted task is stored in PostgreSQL for history and querying
- **Telegram delivery** — sends the briefing as a formatted Markdown message to your personal chat
- **Pluggable adapters** — email fetching and notification are behind port interfaces; swap providers without touching business logic
- **Local testing** — a Mailpit SMTP stub + `test` Spring profile replaces Gmail and OpenAI in development

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    DailyBriefingRunner                  │  ← Spring CommandLineRunner
└────────────────────────┬────────────────────────────────┘
                         │ calls
┌────────────────────────▼────────────────────────────────┐
│                  DailyBriefingService                    │  ← BriefingService interface
│                                                          │
│  ┌──────────────────┐  ┌────────────────┐               │
│  │ EmailFetcherPort │  │ EmailAnalysis  │               │
│  │ (interface)      │  │ Port           │               │
│  └───────┬──────────┘  └───────┬────────┘               │
│          │                     │                         │
│  ┌───────▼──────┐     ┌────────▼──────────┐             │
│  │GmailFetch    │     │ LLMService         │             │
│  │Service (!test│     │ (Ollama/OpenAI)    │             │
│  │MailPitFetch  │     └────────────────────┘             │
│  │Service (test)│                                        │
│  └──────────────┘      ┌──────────────────┐             │
│                         │ NotificationPort │             │
│                         └────────┬─────────┘             │
│                                  │                        │
│                         ┌────────▼─────────┐             │
│                         │TelegramNotification│            │
│                         │Service            │             │
│                         └───────────────────┘             │
└─────────────────────────────────────────────────────────┘
         │                           │
  ┌──────▼──────┐           ┌────────▼──────┐
  │ PostgreSQL  │           │   Telegram    │
  │ (JPA / Fly- │           │   Bot API     │
  │  way)       │           └───────────────┘
  └─────────────┘
```

### Key design decisions

| Pattern | Where used |
|---|---|
| **Ports & Adapters** | `EmailFetcherPort`, `NotificationPort`, `EmailAnalysisPort` decouple business logic from infrastructure |
| **Strategy** | Gmail vs. Mailpit email fetching swapped via Spring `@Profile` |
| **Template Method** | `DailyBriefingService` defines the fixed briefing pipeline; each port can be substituted independently |
| **Builder** | Lombok `@Builder` on all JPA entities for safe, readable construction |
| **Repository** | Spring Data JPA repositories encapsulate all DB access |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| AI | Spring AI · Ollama (prod) · OpenAI-compatible (test) |
| Email | Gmail API v1 (OAuth 2.0) · Mailpit (local SMTP stub) |
| Messaging | Telegram Bot API (TelegramBots 6.9) |
| Database | PostgreSQL 16 · Spring Data JPA · Flyway |
| Build | Gradle 8 (Kotlin DSL) |

---

## Prerequisites

- Java 21+
- Docker & Docker Compose (for PostgreSQL + Mailpit)
- [Ollama](https://ollama.com/) with the `mistral` model pulled locally — **or** a Groq/OpenAI API key (test profile)
- A Gmail account with a Google Cloud project (for production use)
- A Telegram bot token and your personal chat ID

---

## Getting Started

### 1. Clone and configure environment

```bash
git clone https://github.com/vermaankit2005/lumina-ai.git
cd lumina-ai
cp .env.example .env
# Edit .env with your values (see Configuration section below)
```

### 2. Start infrastructure

```bash
docker compose up -d   # starts PostgreSQL (port 5432) and Mailpit (port 8025)
```

### 3. Pull the LLM model

```bash
ollama pull mistral
```

### 4. Run the application

```bash
./gradlew bootRun
```

The application starts, runs the daily briefing pipeline, and exits. Check your Telegram for the briefing message.

---

## Gmail Integration

Lumina uses OAuth 2.0 to access Gmail with read-only scope. Follow these steps once:

### Step 1 — Create a Google Cloud project

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a new project (e.g. `lumina-ai`)
3. Enable the **Gmail API**: *APIs & Services → Enable APIs → Gmail API → Enable*

### Step 2 — Create OAuth 2.0 credentials

1. Go to *APIs & Services → Credentials → Create Credentials → OAuth client ID*
2. Choose **Desktop application** as the application type
3. Download the generated `credentials.json` file

### Step 3 — Configure the path

Place `credentials.json` somewhere on your machine and set the path in `.env`:

```env
GMAIL_CREDENTIALS_FILE_PATH=/path/to/credentials.json
```

### Step 4 — Authorise on first run

On the first run, a browser window opens automatically asking you to sign in and grant Lumina read-only Gmail access. After approval, a `tokens/` directory is created next to the credentials file. Subsequent runs reuse the stored token without opening a browser.

> **Tip:** If the browser window does not open, check the console — the authorisation URL is printed there.

---

## Telegram Integration

### Step 1 — Create a bot

1. Open Telegram and start a chat with **[@BotFather](https://t.me/BotFather)**
2. Send `/newbot` and follow the prompts to choose a name and username
3. Copy the **bot token** shown at the end (format: `123456789:ABCdef...`)

### Step 2 — Find your chat ID

1. Send any message to your new bot
2. Open this URL in a browser (replace `<TOKEN>` with your token):
   ```
   https://api.telegram.org/bot<TOKEN>/getUpdates
   ```
3. In the JSON response, find `"chat": { "id": 123456789 }` — that number is your chat ID

### Step 3 — Configure

```env
TELEGRAM_BOT_TOKEN=123456789:ABCdef...
TELEGRAM_BOT_USERNAME=your_bot_username
TELEGRAM_ALLOWED_CHAT_ID=123456789
```

---

## Local Development (Test Profile)

For development without real Gmail or Telegram credentials:

```bash
# Start Mailpit SMTP stub
docker compose up mailpit -d

# Send some test emails
bash docs/send-test-emails.sh

# Run with the test profile (uses Mailpit + OpenAI-compatible LLM)
SPRING_PROFILES_ACTIVE=test GROQ_API_KEY=<your-groq-key> ./gradlew bootRun
```

Mailpit's web UI is available at [http://localhost:8025](http://localhost:8025).

---

## Configuration Reference

All sensitive values are read from environment variables. Copy `.env.example` to `.env` and fill in your values.

| Variable | Description | Default |
|---|---|---|
| `POSTGRES_HOST` | PostgreSQL hostname | `localhost` |
| `POSTGRES_DB` | Database name | `luminaai` |
| `POSTGRES_USER` | Database user | `lumina` |
| `POSTGRES_PASSWORD` | Database password | *(required)* |
| `GMAIL_CREDENTIALS_FILE_PATH` | Path to `credentials.json` | `credentials.json` |
| `TELEGRAM_BOT_TOKEN` | Bot token from BotFather | *(required)* |
| `TELEGRAM_BOT_USERNAME` | Bot username | `lumina_my_bot` |
| `TELEGRAM_ALLOWED_CHAT_ID` | Your personal Telegram chat ID | *(required)* |
| `OLLAMA_BASE_URL` | Ollama API base URL | `http://localhost:11434` |
| `OLLAMA_MODEL` | Model name to use | `mistral` |
| `GROQ_API_KEY` | API key for OpenAI-compatible endpoint (test profile) | `not-used` |

---

## Running Tests

```bash
./gradlew test
```

Tests use an **H2 in-memory database** and the `test` Spring profile. No external services (Telegram, Gmail, Ollama) are required.

---

## Project Structure

```
src/main/java/com/luminaai/
├── config/            # Spring configuration (Gmail OAuth, LLM client, Telegram)
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
│   ├── briefing/      # BriefingService, DailyBriefingService, BriefingFormatter
│   ├── gmail/         # GmailFetchService, MailPitFetchService
│   └── notification/  # TelegramNotificationService
└── telegram/          # LuminaTelegramBot (inbound message handling)

src/main/resources/
├── db/migration/      # Flyway SQL migrations
└── prompts/           # LLM system and user prompt templates
```

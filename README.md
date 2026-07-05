# Shopping Made Better

> A smarter grocery shopping companion for Android — build shopping lists, compare
> prices across stores, track your pantry, and plan meals, all in one place.

*Shopping Made Better* is the Full Sail University Capstone project of `<team name here>`.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Technologies](#technologies)
- [Installation](#installation)
- [Development Setup](#development-setup)
  - [Setup Supabase Locally](#setting-up-supabase-locally)
  - [Database Seed Data](#seed-data)
- [Project Status](#project-status)
- [Roadmap](#roadmap)
- [Known Issues](#known-issues)
- [Contributors](#contributors)
- [License](#license)
- [Additional Resources](#additional-resources)

## Introduction

Grocery shopping is fragmented: prices differ from store to store, pantry staples run
out unnoticed, and meal planning rarely connects to the list you actually take to the
store. **Shopping Made Better** brings these pieces together in a single native Android
app. It helps shoppers build smart shopping lists, see where items are cheapest, keep
an inventory of what's already in the pantry, review past purchases, and plan meals —
so a weekly grocery run costs less time and less money.

The app is backed by real grocery pricing data, letting it surface meaningful
price comparisons rather than guesses.

## Features

The app is organized around five top-level areas:

- **Shopping Lists** — create and manage grocery lists for your next trip.
- **Cart** — track items you're actively buying and see running totals.
- **Pantry** — keep an inventory of what you already have at home.
- **History** — review past shopping trips and purchases.
- **Meals** — plan meals and generate the ingredients you need.

> **Note:** The project is in active early development. Several screens are currently
> placeholders while the underlying navigation, theming, database, and dependency
> injection are built out. See [Project Status](#project-status).

## Technologies

**Android app**

- **[Kotlin](https://kotlinlang.org/)** — primary language
- **[Jetpack Compose](https://developer.android.com/develop/ui/compose)** with
  **Material 3** — declarative UI and theming
- **[Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)** —
  type-safe, serializable navigation destinations
- **[Dagger Hilt](https://dagger.dev/hilt/)** — dependency injection
- **[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)** —
  serializable navigation routes and data models
- **Gradle** (Kotlin DSL) with a version catalog (`gradle/libs.versions.toml`)

**Backend & data**

- **[Supabase](https://supabase.com/)** (Postgres, Auth, Storage) — database and
  authentication, run locally during development
- **[supabase-kt](https://github.com/supabase-community/supabase-kt)** (Postgrest,
  Auth) with the **[Ktor](https://ktor.io/)** HTTP client — Supabase access from Android
- **Python** — seed-data generation (`scripts/generate_seed.py`) from grocery pricing CSVs

**Tooling**

- **GitHub Actions** — Android CI (build & test on every push/PR)
- **Docker / Rancher / Podman** — container runtime for the local Supabase stack

## Installation

> Shopping Made Better is a native Android application and is not yet published to the
> Google Play Store. During the Capstone development phase, it is installed by building
> from source (see [Development Setup](#development-setup)).

**Requirements to run the app:**

- An Android device or emulator running **Android 11 (API 30)** or newer.

**To install a build:**

1. Build the app from source using the [Development Setup](#development-setup) steps
   below, or obtain a signed `.apk` from a project maintainer.
2. Enable installing apps from your build source (Android Studio handles this
   automatically when deploying to a connected device or emulator).
3. Run the app from Android Studio, or install the APK with `adb install app.apk`.

## Development Setup

New developer getting set up? This section covers everything needed to build and run
the project from a fresh clone.

### Prerequisites

- **[Android Studio](https://developer.android.com/studio)** (latest stable) with the
  Android SDK — the project targets **API 37** and requires a minimum of **API 30**.
- **[Node.js](https://nodejs.org/)** — used to run the Supabase CLI via `npx`.
- **[Python 3](https://www.python.org/)** — used to regenerate seed data (optional).
- One container runtime (required for Supabase):
  - [Docker Desktop](https://docs.docker.com/desktop/) (macOS, Windows, Linux) — **(recommended)**
  - [Rancher Desktop](https://rancherdesktop.io/) (macOS, Windows, Linux)
  - [Podman](https://podman.io/) (macOS, Windows, Linux)

### Clone and Build the App

```bash
git clone https://github.com/byte2pixel/shopping-made-better.git
cd shopping-made-better
```

Open the project in Android Studio and let Gradle sync, or build from the command line:

```bash
./gradlew assembleDebug     # macOS / Linux
gradlew.bat assembleDebug   # Windows
```

Run the app on a connected device or emulator from Android Studio, or:

```bash
./gradlew installDebug
```

### Setting Up Supabase Locally

We use Supabase for local database development. All team members develop against their
own local database. The initial setup and config has been done, so there's no need to run
`npx install supabase --save-dev` or `npx supabase init`.

#### 1. Install the Supabase CLI

```bash
npm install
```

#### 2. Start the Local Supabase Stack

```bash
npx supabase start -x vector
```

First run takes ~2 min. Services (Postgres, Auth, Storage) start in Docker containers.

If you see an error `failed to inspect service: error during connect:` it likely means
Docker is not running. Start Docker Desktop and try again.
The vector service is a pro feature. That is why we start with `-x vector`.

#### 3. Access Local Supabase

Open a browser to [http://localhost:54323](http://localhost:54323).

#### 4. View Database Credentials

After `supabase start`, credentials print to the console. Save them — they should be the
same each time you start Supabase locally. The credentials are also in the
`supabase/.env` file.

```
╭──────────────────────────────────────╮
│ Development Tools                    │
├─────────┬────────────────────────────┤
│ Studio  │ http://127.0.0.1:54323     │
│ Mailpit │ http://127.0.0.1:54324     │
│ MCP     │ http://127.0.0.1:54321/mcp │
╰─────────┴────────────────────────────╯

╭──────────────────────────────────────────────────────╮
│  APIs                                                │
├────────────────┬─────────────────────────────────────┤
│ Project URL    │ http://127.0.0.1:54321              │
│ REST           │ http://127.0.0.1:54321/rest/v1      │
│ GraphQL        │ http://127.0.0.1:54321/graphql/v1   │
│ Edge Functions │ http://127.0.0.1:54321/functions/v1 │
╰────────────────┴─────────────────────────────────────╯

╭───────────────────────────────────────────────────────────────╮
│ Database                                                      │
├─────┬─────────────────────────────────────────────────────────┤
│ URL │ postgresql://postgres:postgres@127.0.0.1:54322/postgres │
╰─────┴─────────────────────────────────────────────────────────╯

╭──────────────────────────────────────────────────────────────╮
│ Authentication Keys                                          │
├─────────────┬────────────────────────────────────────────────┤
│ Publishable │ sb_publishable_AC***************************** │
│ Secret      │ sb_secret_N7*****************************      │
╰─────────────┴────────────────────────────────────────────────╯

╭───────────────────────────────────────────────────────────────────────────────╮
│ Storage (S3)                                                                  │
├────────────┬──────────────────────────────────────────────────────────────────┤
│ URL        │ http://127.0.0.1:54321/storage/v1/s3                             │
│ Access Key │ 62******************************                                 │
│ Secret Key │ 85************************************************************** │
│ Region     │ local                                                            │
╰────────────┴──────────────────────────────────────────────────────────────────╯
```

We likely only need the REST URL and Authentication Keys for our project. The REST URL is
the endpoint we use to make requests to our database. The Publishable and Secret keys are
used for authentication when making those requests.

#### 5. Point the Android app at your local database

The app reads the Supabase URL and key from `local.properties` (git-ignored — no
secrets are committed). After `supabase start`, add the **Publishable** key and
the URL to `local.properties`:

```properties
SUPABASE_ANON_KEY=sb_publishable_...        # the "Publishable" key printed above
SUPABASE_URL=http://192.168.1.50:54321      # your computer's LAN IP (see below)
```

**Emulator (recommended): use your host machine's LAN IP.** The local Supabase
stack is published on all interfaces, so the emulator can reach it at your
computer's LAN IP. Find it with `ipconfig` on Windows (the IPv4 Address, e.g.
`192.168.1.50`) or `ifconfig`/`ip addr` on macOS/Linux, then set
`SUPABASE_URL=http://<that-ip>:54321`.

> Note: the built-in emulator alias `http://10.0.2.2:54321` is the code default,
> but on **Windows + Docker Desktop** it is unreliable — Docker's port proxy
> accepts the TCP connection but doesn't forward it from the emulator's loopback
> route, so requests time out. Using the LAN IP avoids this.

**Physical device over USB:** forward the port and point at localhost:

```bash
adb reverse tcp:54321 tcp:54321   # re-run after each reconnect/reboot
```
```properties
SUPABASE_URL=http://127.0.0.1:54321
```

These values are injected into `BuildConfig` at build time by `app/build.gradle.kts`.
If `SUPABASE_ANON_KEY` is missing, the app builds but cannot reach the database.

#### Stop/Reset

```bash
npx supabase stop              # Stop containers (keep data)
npx supabase stop --no-backup  # Stop without saving state
npx supabase reset             # Wipe database, restart
```

### Benefits of Local Development

- **Instant feedback** — no deploy wait
- **Offline work** — no internet needed after setup
- **Free** — no quota consumption
- **Privacy** — sensitive data stays local
- **Easy testing** — reset the database anytime

### Migrations

Database schema changes go in `supabase/migrations/`. The CLI auto-runs them on `start`.

### Seed Data

Seed data lives in `supabase/seed.sql`, generated by `scripts/generate_seed.py` from the
grocery pricing data in `grocery_data/`. This populates the database with initial stores,
products, and pricing information.

Generate the seed SQL:

```bash
py scripts/generate_seed.py
```

Seed the database — with the Supabase containers started, run:

```bash
npx supabase db reset
```

### Project Paths

Key locations in the repository:

| Path                       | Purpose                                            |
| -------------------------- | -------------------------------------------------- |
| `app/`                     | Android application module (Kotlin + Compose)      |
| `app/src/main/java/com/fullsail/shoppingmadebetter/` | App source (UI, navigation, DI) |
| `gradle/libs.versions.toml`| Gradle version catalog (dependency versions)       |
| `supabase/migrations/`     | Database schema migrations                         |
| `supabase/seed.sql`        | Generated seed data                                |
| `scripts/generate_seed.py` | Seed-data generator                                |
| `grocery_data/`            | Source grocery pricing CSVs                        |

## Project Status

**Alpha: active development.** Shopping Made Better is an in-progress Full Sail
University Capstone project. Core infrastructure is in place — app navigation, Material 3
theming, Dagger Hilt dependency injection, local Supabase database with migrations and
seed data, and Android CI. Several feature screens are currently placeholders while the
individual features are built out.

## Roadmap

- [x] App navigation with top/bottom bars and type-safe destinations
- [x] Material 3 theme
- [x] Local Supabase database with migrations and seed data
- [x] Dependency injection with Dagger Hilt
- [ ] User authentication (Supabase Auth)
- [ ] Shopping Lists
- [ ] Cart and price tracking
- [ ] Pantry inventory
- [ ] Purchase History
- [ ] Meal (planning / Recommendations)

## Known Issues

- The five main tab screens (Shopping Lists, Cart, Pantry, History, Meals) are currently
  placeholders pending feature implementation.
- Authentication is not yet wired up; the Login screen is a placeholder gate.

## Contributors

Shopping Made Better is built and maintained by:

| Name | GitHub |
|------|--------|
| JourdynLuv | [@JourdynLuv](https://github.com/jourdynluv) |
| Mel Dommer | [@byte2pixel](https://github.com/byte2pixel) |
| Taffy | [@sour-taffy](https://github.com/sour-taffy) |

Contributions from teammates and collaborators are reflected in the project's
[commit history](https://github.com/byte2pixel/shopping-made-better/commits).

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for
the full text. For more on choosing an open-source license, see
[choosealicense.com](https://choosealicense.com/).

## Additional Resources

**Supabase**
- [Supabase CLI reference](https://supabase.com/docs/reference/cli/introduction)
- [Supabase migrations docs](https://supabase.com/docs/reference/cli/supabase-migration)

**Jetpack Compose**
- [Jetpack Compose documentation](https://developer.android.com/develop/ui/compose/documentation)

**Dagger Hilt**
- [Hilt dependency injection guide](https://developer.android.com/training/dependency-injection/hilt-android)

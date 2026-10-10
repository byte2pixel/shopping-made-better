# Shopping Made Better

> A smarter grocery shopping companion for Android — build shopping lists, compare
> prices across stores, track your pantry, and plan meals, all in one place.

*Shopping Made Better* is the Full Sail University Capstone project of team C202610-01.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Technologies](#technologies)
- [Installation](#installation)
- [Development Setup](#development-setup)
  - [Setup Supabase Locally](#setting-up-supabase-locally)
  - [Database Seed Data](#seed-data)
  - [Demo Accounts](#demo-accounts)
  - [Testing](#testing)
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

The app is organized around four tabs, backed by shared accounts and households:

- **Shopping Lists** — build lists for your next trip, compare an item's price
  across stores, and complete a list to record it as a shopping trip. Lists are
  shared with everyone in your household.
- **Pantry** — track what you have at home by location (pantry, fridge, freezer)
  with expiry estimates, low-stock and out-of-stock alerts, and quantity estimates
  based on how often you buy an item. An optional nightly job lowers quantities
  for you, and a weekly digest lets you review or undo each adjustment.
- **History** — browse every completed trip, search and filter by store and date,
  switch between your own trips and the whole household's, open a trip's line
  items, and see spend insights.
- **Meals** — search meals and ingredients, open a recipe, add its ingredients to
  a shopping list, and save your own recipes.
- **Accounts & households** — email sign-up and sign-in with Supabase Auth, an
  onboarding flow for dietary, category, goal and auto-adjust preferences, a
  profile screen, and households that members join with an invite code.

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
- **[kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)** — dates for
  expiry, trips and adjustments
- **[Coil](https://coil-kt.github.io/coil/)** — product image loading
- **[Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)** —
  device-level settings
- **[Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)** —
  paged purchase history
- **Gradle** (Kotlin DSL) with a version catalog (`gradle/libs.versions.toml`)

**Backend & data**

- **[Supabase](https://supabase.com/)** (Postgres, Auth, Storage) — database and
  authentication, run locally during development
- **[supabase-kt](https://github.com/supabase-community/supabase-kt)** (Postgrest,
  Auth) with the **[Ktor](https://ktor.io/)** HTTP client — Supabase access from Android
- **[pg_cron](https://github.com/citusdata/pg_cron)** — nightly pantry consumption job
- **[pgTAP](https://pgtap.org/)** — database tests in `supabase/tests/`
- **Python** — seed-data generation (`scripts/generate_seed.py` and
  `scripts/shelf_life.py`) from grocery pricing CSVs

**Tooling**

- **GitHub Actions** — Android CI builds the debug APK on every PR and push to
  `main`; unit tests run on the Weekly Tests workflow (Mondays) or on demand
- **Docker / Rancher / Podman** — container runtime for the local Supabase stack

**Expected Storage Requirements**
- ~50mb

## Installation

> Shopping Made Better is a native Android application and is not yet published to the
> Google Play Store. During the Capstone development phase, it is installed by building
> from source (see [Development Setup](#development-setup)).

**Requirements to run the app:**

- An Android device or emulator running **Android 11 (API 30)** or newer.

**To install a build:**

1. Build the app from source using the [Development Setup](#development-setup) steps
   below, or download the signed `.apk` attached to the latest
   [GitHub Release](https://github.com/byte2pixel/shopping-made-better/releases),
   which is built against the cloud Supabase project.
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
`npm install supabase --save-dev` or `npx supabase init`.

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
same each time you start Supabase locally.

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
npx supabase db reset          # Wipe database, re-run migrations and seeds
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

`supabase/seed.sql` is generated and git-ignored, so a fresh clone has none and
`db reset` fails until you create it. `scripts/generate_seed.py` builds it from the
grocery pricing data in `grocery_data/`: three stores (Whole Foods, ALDI and Publix)
with per-store price variations, the product catalog, and a default shelf life per
product assigned by `scripts/shelf_life.py`, which the pantry uses to estimate expiry.

The `grocery_data/` used was sourced from [Kaggle Grocery Data](https://www.kaggle.com/datasets/maximsakhan/rc-superstore-grocery-data)
The `.csv` in `grocery_data/` in this repo carries the Apache 2.0 License and was not modified.

1. Generate the seed SQL:

   ```bash
   py scripts/generate_seed.py
   ```

2. Seed the database — with the Supabase containers started, run:

   ```bash
   npx supabase db reset
   ```

Re-run both steps whenever the generator or the CSV changes.

#### Resetting the cloud database

Four workflows under **Actions** touch the cloud project:

| Workflow | When |
|----------|------|
| Release APK | Runs when a GitHub Release is published; builds a signed APK against the cloud project from the `production` environment's `SUPABASE_URL` variable, `SUPABASE_ANON_KEY` secret and `RELEASE_KEYSTORE_*` secrets, and attaches it to the release. A manual run only uploads a workflow artifact. |
| Deploy Supabase Migrations | Runs when a GitHub Release is published; applies new migrations only. Safe to re-run. |
| Seed Supabase (one-time) | First catalog load after the first deploy. Not re-runnable. |
| Reset Supabase (destructive, pre-launch only) | Drops the cloud database and rebuilds it from the migrations and both seed files, like a local `db reset`. Every row and every account is lost. |

Reset asks for the phrase `WIPE CLOUD DATABASE` and refuses anything else. It exists to
carry seed changes to the cloud project **before launch**; once real users have data it
must never be run.

### Demo Accounts

A fresh local database comes with two demo users in a shared household, each
with a pantry, shopping lists and purchase history, so the app has data to show
for development and demos. They are defined in `supabase/dummy_account.sql` and
loaded on every `npx supabase db reset` via the `[db.seed] sql_paths` list in
`supabase/config.toml` (loaded **after** `seed.sql` so its rows can reference the
seeded stores and products).

The seed also runs the consumption estimator and the auto-adjustment job, so the
demo account starts with a week of automatic adjustments for the pantry digest to
list — one of them already undone.

**Sign-in credentials** (email confirmation is disabled locally, so this works
immediately from the app's sign-in screen):

| Account                 | Email            | Password      |
|-------------------------|------------------|---------------|
| Demo Shopper (head)     | `demo@smb.test`  | `password123` |
| Demo Roommate (member)  | `demo2@smb.test` | `password123` |

Both accounts are in "Demo Household", invite code `DEMO2026`. The demo
account also seeds pantry items, two shopping lists and twelve completed trips;
the roommate seeds two lots, one shopping list and three trips, so either
account's Lists and History tabs show a housemate's data.

> **Note:** This is throwaway local test data — never commit real credentials or
> use it for anything deployed.

#### Removing the demo accounts

If you no longer want the demo data:

1. Delete `supabase/dummy_account.sql`.
2. Remove `"./dummy_account.sql"` from the `sql_paths` list under `[db.seed]` in
   `supabase/config.toml` (leaving `sql_paths = ["./seed.sql"]`).
3. Run `npx supabase db reset` to rebuild the database without the dummy data.

### Project Paths

Key locations in the repository:

| Path                                                 | Purpose                                                                 |
|------------------------------------------------------|-------------------------------------------------------------------------|
| `app/`                                               | Android application module (Kotlin + Compose)                           |
| `app/src/main/java/com/fullsail/shoppingmadebetter/` | App source: `core/` (shared DI, domain, UI) and `feature/<name>/` with `data`, `domain`, `ui` and `di` packages |
| `app/src/test/`                                      | Unit tests                                                              |
| `docs/adding-a-use-case.md`                          | Cookbook for adding a screen → view model → use case → repository slice |
| `gradle/libs.versions.toml`                          | Gradle version catalog (dependency versions)                            |
| `.github/workflows/`                                 | Android CI, weekly tests and the cloud Supabase workflows               |
| `supabase/migrations/`                               | Database schema migrations                                              |
| `supabase/tests/`                                    | pgTAP database tests                                                    |
| `supabase/seed.sql`                                  | Generated seed data (git-ignored)                                       |
| `supabase/dummy_account.sql`                         | Demo accounts + mock data (removable)                                   |
| `scripts/generate_seed.py`                           | Seed-data generator                                                     |
| `scripts/shelf_life.py`                              | Shelf-life classifier used by the generator                             |
| `grocery_data/`                                      | Source grocery pricing CSVs                                             |

### Testing

```bash
./gradlew testDebugUnitTest   # Android unit tests
npx supabase test db          # pgTAP tests in supabase/tests/ (stack must be running)
```

## Project Status

**Feature complete for the Capstone.** All four tabs, authentication, onboarding and
household sharing are implemented, and a cloud Supabase project receives migrations
on every published release. Development wraps up in October 2026.

## Roadmap

- [x] App navigation with top/bottom bars and type-safe destinations
- [x] Material 3 theme
- [x] Local Supabase database with migrations and seed data
- [x] Dependency injection with Dagger Hilt
- [x] User authentication (Supabase Auth)
- [x] Onboarding preferences
- [x] Shopping Lists
- [x] Price comparison across stores
- [x] Pantry inventory
- [x] Automatic pantry adjustments with a weekly digest
- [x] Purchase History
- [x] Meals and recipes
- [x] Household sharing

## Known Issues

- On Windows with Docker Desktop the emulator cannot reach Supabase through
  `10.0.2.2`; use your host's LAN IP as described in
  [Point the Android app at your local database](#5-point-the-android-app-at-your-local-database).
- Product images fall back to a placeholder when the emulator's DNS is broken;
  Supabase still works because its URL is an IP literal.
- PR CI does not run unit tests, so a merged PR can leave tests failing on `main`
  until the weekly run catches it.

## Contributors

Shopping Made Better is built and maintained by:

| Name       | GitHub                                       | Role      |
|------------|----------------------------------------------|-----------|
| JourdynLuv | [@JourdynLuv](https://github.com/jourdynluv) | Developer |
| Mel Dommer | [@byte2pixel](https://github.com/byte2pixel) | Developer |
| Taffy      | [@sour-taffy](https://github.com/sour-taffy) | Developer |

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

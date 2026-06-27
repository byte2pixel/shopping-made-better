# shopping-made-better

Shopping Made Better - Full Sail University Capstone Project

## Local Development Setup

### Prerequisites

Install one container runtime (required for Supabase):

- [Docker Desktop](https://docs.docker.com/desktop/) (macOS, Windows, Linux) — **(recommended)**
- [Rancher Desktop](https://rancherdesktop.io/) (macOS, Windows, Linux)
- [Podman](https://podman.io/) (macOS, Windows, Linux)

### Setting Up Supabase Locally

We use Supabase for local database development. All team members develop against their own local database.  The initial setup and config has been done.  So no need to run `npx install supabase --save-dev` or `npx supabase init`.

#### 1. Install Supabase CLI

```bash
npm install
```

#### 2. Start Local Supabase Stack

```bash
npx supabase start -x vector
```

First run takes ~2 min. Services (Postgres, Auth, Storage) start in Docker containers.

If you see an error `failed to inspect service: error during connect:` likely means Docker is not running. Start Docker Desktop and try again.
The vector service is a pro feature. That is why we start with `-x vector`.

#### 3. Access Local Supabase

Open browser: [http://localhost:54323](http://localhost:54323)

#### 4. View Database Credentials

After `supabase start`, credentials print to console. Save them, they should be the same each time you start Supabase locally.  The credentials are also in `supabase/.env` file.

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

We likely only need the REST URL and Authentication Keys for our project.  The REST URL is the endpoint we will use to make requests to our database.  The Publishable and Secret keys are used for authentication when making requests to the database.

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
- **Easy testing** — reset database anytime

### Migrations

Database schema changes go in `supabase/migrations/`. CLI auto-runs them on `start`.

- See [Supabase migrations docs](https://supabase.com/docs/guides/local-development/cli/managing-schemas#create-migrations) for details.
- See [Supabase CLI reference](https://supabase.com/docs/guides/local-development/cli) for more.
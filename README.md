# ReturnLoop

**Less Waste. More Value.**

ReturnLoop is an intelligent reverse-logistics and circular-commerce platform. It tracks returned
and unsold goods through a full lifecycle, calculates **explainable** recovery economics, plans
capacity-constrained collection routes, and reports the value recovered and the waste diverted.

It is a working full-stack application: a Spring Boot 3.4 backend on PostgreSQL, and a React 18 +
TypeScript + Vite frontend that talks to the real API.

---

## What it does

The product answers the questions a reverse-logistics operation actually has:

- **What should happen to a returned product?** Eight recovery destinations are evaluated for every
  item — restock as new, open-box resale, repair, refurbishment, liquidation, spare parts,
  recycling and safe disposal.
- **Which destination recovers the most value?** Net recovery value is computed from a documented
  cost model: `expectedResaleValue − repairCost − inspectionCost − handlingCost −
  transportationCost − partnerFees`.
- **Can nearby pickups be consolidated?** A capacity-constrained nearest-neighbour solver with a
  2-opt improvement pass groups collections into vehicle routes.
- **Which partner can do the work fastest?** Partner matching uses recorded performance: completion
  counts, on-time rate and average turnaround, all derived from real jobs.
- **How much value was recovered, and how much waste avoided?** Every figure on the dashboard comes
  from persisted records.

The lifecycle is fully navigable in the UI:

```
Return Created → Pickup Scheduled → Collected → Warehouse Received → Inspected
   → Recovery Decision → Partner Assigned → Processing → Recovered → Completed
```

---

## Architecture at a glance

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA, Hibernate, Flyway, Bean Validation, Actuator, Micrometer, springdoc |
| Security | Spring Security, stateless JWT (jjwt), BCrypt, organisation-scoped queries |
| Data | PostgreSQL 16, Redis 7, Apache Kafka 3.9 |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, TanStack Query, React Hook Form, Zod, Recharts, React Router, Lucide |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, Vitest, Testing Library |
| Infrastructure | Docker, Docker Compose, GitHub Actions |

Full detail: [`docs/architecture.md`](docs/architecture.md) · [`docs/erd.md`](docs/erd.md) ·
[`docs/api.md`](docs/api.md)

---

## Running it locally on Windows

### Quick local run (no Docker)

For a single-machine demo, use the `local` Spring profile. It stores records in the file-backed
H2 database at `backend/data/returnloop` and seeds the demo workspace. The database file is ignored
by Git and survives backend restarts. Password reset codes are sent through SMTP; Docker Compose
provides a local Mailpit inbox at **http://localhost:8025**.

In one PowerShell window from the repository root:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
..\mvnw.cmd spring-boot:run
```

In a second window:

```powershell
cd frontend
npm run dev
```

Open **http://localhost:5173**. Start Mailpit with `docker compose up -d mailpit` to try password
reset locally. For production, set `SMTP_HOST`, `SMTP_PORT`, `SMTP_AUTH`, `SMTP_STARTTLS`,
`SMTP_USERNAME`, `SMTP_PASSWORD` and `RETURNLOOP_MAIL_FROM` for your email provider. For MySQL, set
`SPRING_PROFILES_ACTIVE=mysql`, `DB_USERNAME` and `DB_PASSWORD`; Maven must be able to resolve the
MySQL Connector/J dependency declared by the backend.

The login page uses email and password. Password reset sends a one-time code by email, so configure
the SMTP settings above before enabling account recovery in a deployed environment.

### Production container deployment

The production Compose file builds and runs the API and frontend with PostgreSQL and Redis. It
requires deployment-specific credentials and does not seed demo accounts. Copy
`.env.production.example` to `.env.production`, replace every placeholder with strong values, and set
`RETURNLOOP_CORS_ORIGINS` to the exact public site origin. Then run:

```powershell
docker compose --env-file .env.production -f docker-compose.production.yml up --build -d
```

The site listens on `HTTP_PORT` (8081 by default); the API is only reachable through the frontend
proxy. Put the site behind a TLS-enabled reverse proxy before exposing it publicly, and use your
SMTP provider for password reset codes. Flyway applies database migrations on backend startup. Back
up the `returnloop-postgres` volume before upgrades.

### Prerequisites

* **Java 21+** — check with `java -version`
* **Node.js 20+** — check with `node -v`
* **Docker Desktop** — check with `docker --version`
* **Maven** is *not* required: the repository ships a wrapper (`mvnw.cmd`).

### 1. Start the infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL, Redis and Kafka. Check they are healthy:

```bash
docker compose ps
```

### 2. Configure the environment

```bash
copy .env.example .env
```

Then open `.env` and set a real secret:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Paste the result into `RETURNLOOP_JWT_SECRET`. The application refuses to start if the secret is
shorter than 32 bytes.

### 3. Start the backend

In PowerShell, from the repository root:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "dev"
..\mvnw.cmd spring-boot:run
```

The API is on **http://localhost:8080** and Swagger UI on
**http://localhost:8080/swagger-ui.html**.

On first start Flyway creates the schema and the seeder loads a demo organisation, catalogue,
fleet and partner directory.

### 4. Start the frontend

In a second PowerShell window:

```powershell
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The dev server proxies `/api` to the backend, so no CORS setup is
needed during development.

### 5. Sign in

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `ops@northwind-retail.example` | `ReturnLoop!2026` |
| Warehouse inspector | `warehouse@northwind-retail.example` | `ReturnLoop!2026` |
| Logistics planner | `logistics@northwind-retail.example` | `ReturnLoop!2026` |
| Analyst | `analyst@northwind-retail.example` | `ReturnLoop!2026` |

Change the demo password with `RETURNLOOP_DEMO_PASSWORD` in `.env`.

---

## Walkthrough: the vertical slice

With the app running and the demo account signed in:

1. **Create a return** — *Returns Management → Create return*. Add a SKU from the seeded catalogue
   (`NWR-PH-0142`, `NWR-LP-0311`, …), a reason and a customer. The return is created with a
   generated number such as `RET-2026-00001`.
2. **Inspect the item** — *Inspections → Open an inspection*. Pick the return, open an inspection
   for one of its items, record packaging condition, functionality and accessories, then complete
   it. A completed inspection is locked.
3. **Compare recovery paths** — *Recovery Engine*. Select the return and generate a recommendation.
   Every path is listed with its gross value, each cost line, net recovery value, turnaround, data
   quality and the reasons any path was ruled out. Approve or reject with a reason.
4. **Schedule a pickup** — *Pickup Scheduling → Schedule pickup*. Give it an origin, coordinates, a
   date and a destination warehouse.
5. **Plan the route** — *Route Optimization*. Pick the date and run the optimiser. Stops are
   sequenced, distance and duration are estimated, and capacity is checked.
6. **Assign a partner** — *Partner Marketplace*, open a partner, then assign a recovery job.
7. **Complete the job** — *Partner Jobs*. Walk it through Assigned → Accepted → Processing →
   Awaiting review → Completed, recording the realised value.
8. **Watch the dashboard** — the KPIs, the recovery distribution and the trend chart all move,
   because every number is computed from the records you just created.

---

## Commands

### Backend (from `backend/`)

```bash
# Compile
..\mvnw.cmd -DskipTests compile

# Run the test suite
..\mvnw.cmd test

# Package a runnable jar
..\mvnw.cmd -DskipTests package
java -jar target\returnloop.jar

# Run with the dev profile
..\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend (from `frontend/`)

```bash
npm install
npm run dev        # dev server on :5173
npm run build      # type check + production build into dist/
npm run preview    # serve the production build
npm run lint       # TypeScript type check only
npm test           # Vitest suite
```

---

## Project layout

```
project/
├── backend/                      Spring Boot application
│   ├── src/main/java/com/returnloop/
│   │   ├── common/               error envelope, events, idempotency, base entities
│   │   ├── configuration/        externalised properties, OpenAPI
│   │   ├── security/             JWT, filters, roles
│   │   ├── identity/             users, authentication
│   │   ├── organization/         the tenant
│   │   ├── catalog/              products and SKUs
│   │   ├── returns/              the lifecycle
│   │   ├── inventory/            stock, movements, ageing
│   │   ├── inspection/           the warehouse workspace
│   │   ├── recovery/             the decision engine
│   │   ├── logistics/            pickups, vehicles, routes
│   │   ├── partner/              directory and jobs
│   │   ├── analytics/            aggregations
│   │   ├── notification/         in-app notifications
│   │   ├── audit/                the audit trail
│   │   ├── imports/              CSV ingestion
│   │   └── bootstrap/            the demo seeder
│   └── src/main/resources/db/migration/   Flyway migrations
├── frontend/                     React application
│   └── src/
│       ├── api/                  typed API client
│       ├── auth/                 session state
│       ├── components/           design system, charts, map
│       ├── layout/               shell, sidebar, header, search
│       ├── lib/                  api transport, formatting, types
│       └── pages/                one module per screen
├── docs/                         architecture, ERD, API reference
├── docker-compose.yml            PostgreSQL, Redis, Kafka
└── .env.example                  every setting, no secrets
```

---

## Design system

| Token | Value | Use |
| --- | --- | --- |
| Jet Black | `#0A0A0A` | Application background |
| Cerulean Blue | `#007BA7` | Navigation, controls, borders, charts, selected states |
| Neon Rose | `#FF2D95` | High-priority actions, active indicators, accents |
| Card surface | `#101318` | Panels and cards |
| Elevated surface | `#151A22` | Modals, menus, table headers |
| Subtle border | `#202A35` | Dividers and outlines |
| Primary text | `#F5F7FA` | Body copy |
| Secondary text | `#A4AFBF` | Supporting copy |
| Success / Warning / Error | `#00D6A3` / `#FFB547` / `#FF5263` | Semantic status only |

Inter for type, Lucide for icons, 12–18 px card radii, 1 px borders at low contrast, and a single
focus ring in cerulean blue throughout.

---

## What is implemented

* Full return lifecycle with a validated state machine, item management, timeline and audit.
* Warehouse inspection workspace with immutable completed inspections and attachments.
* Explainable recovery engine: eight paths, documented cost model, eligibility rules, ranked
  comparison, approve/reject with a reason.
* Pickup scheduling, vehicle and driver assignment, capacity checks, failed-pickup handling.
* Route optimisation: capacity-constrained nearest-neighbour with 2-opt, over-capacity flagged
  rather than silently dropped.
* Partner directory with recorded performance, matching, job assignment, duplicate-assignment
  protection and a nine-state job lifecycle.
* Inventory with movement history, reservation, dispatch, non-negative stock enforcement and a
  holding-time monitor.
* CSV import with per-row validation and an import report.
* Analytics, reporting, environmental impact with stated methodology, notifications, audit trail,
  users and roles, organisation settings.
* JWT auth, organisation-scoped data isolation, idempotency keys, optimistic locking, uniform
  error envelope, OpenAPI documentation, health checks and metrics.

## Known limitations

These are deliberate, and the product is explicit about each one in the UI:

* **The map is simulated.** It is a projection of the coordinates stored on each pickup, clearly
  labelled as such. Set `RETURNLOOP_MAP_API_KEY` to wire in a real tile provider.
* **Distances are estimates.** Great-circle distances with a flat average speed and a synthetic rate
  card. They are never presented as live navigation.
* **Environmental figures are estimates.** Modelled from product weights and published
  embodied-carbon factors. The methodology is displayed alongside every figure.
* **The recovery engine is rule-based.** It is described as exactly that, never as a trained model.
  The `RecoveryRankingStrategy` interface is the seam for a learned model.
* **Partner metrics start empty.** They are computed from completed jobs, so a freshly seeded
  partner shows no performance until it has done work. No metric is fabricated.
* **Kafka is optional.** Events are dispatched in-process by default; enable Kafka with
  `RETURNLOOP_KAFKA_ENABLED=true`.
* **The inventory CSV import reports its errors** rather than writing rows; register the products
  first, then re-run.
* **Testcontainers-based integration tests** are scaffolded but are skipped in CI unless Docker is
  available to the runner.

---

## License

Proprietary. All rights reserved.

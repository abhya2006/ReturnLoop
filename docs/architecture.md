# ReturnLoop — Architecture

## 1. Shape of the system

ReturnLoop is a **modular monolith**: one deployable Spring Boot application, one PostgreSQL
database, with strict package boundaries per bounded context. The contexts are deliberately
separated so that the recovery, logistics or partner modules can be extracted into services later
without rewriting the others.

```
┌───────────────────────────────────────────────────────────────────────┐
│                          Browser (React + Vite)                        │
│   Design system · TanStack Query · React Router · Recharts · Lucide  │
└───────────────────────────────┬───────────────────────────────────────┘
                                │ HTTPS / JSON, JWT bearer
┌───────────────────────────────▼───────────────────────────────────────┐
│                     Spring Boot 3.4 · Java 21                          │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────────┐   │
│  │  identity  │ │  returns   │ │ inventory  │ │    inspection    │   │
│  ├────────────┤ ├────────────┤ ├────────────┤ ├──────────────────┤   │
│  │  recovery  │ │  logistics │ │  partner   │ │     analytics    │   │
│  └────────────┘ └────────────┘ └────────────┘ └──────────────────┘   │
│  common: error envelope · domain events · idempotency · config        │
└───────┬───────────────────────┬───────────────────────┬───────────────┘
        │                       │                       │
┌───────▼────────┐   ┌──────────▼────────┐   ┌──────────▼────────┐
│   PostgreSQL   │   │      Redis        │   │      Kafka        │
│   (primary)    │   │  (cache/sessions) │   │ (async workflows) │
└────────────────┘   └───────────────────┘   └───────────────────┘
```

Every context follows **Controller → Service → Repository**. Controllers never touch JPA entities;
they exchange DTOs. Services own the business rules and the transaction boundaries. Repositories
own persistence only.

## 2. Package structure

```
com.returnloop
├── common            cross-cutting: base entities, error envelope, events, idempotency, config
├── configuration     Spring configuration and externalised properties
├── security          JWT issuing/validation, filters, the current-user accessor
├── identity          user accounts, roles, authentication
├── organization      the tenant
├── catalog           products and SKUs
├── returns           the reverse-logistics lifecycle — the core aggregate
├── inventory         stock on hand, movements, holding-time monitor
├── inspection        warehouse inspection workspace
├── recovery          the recovery decision engine
├── logistics         pickups, vehicles, drivers, routes
├── partner           partner directory and recovery jobs
├── analytics         dashboard and reporting aggregations
├── notification      in-app notifications
├── audit             the audit trail
├── imports           CSV ingestion
└── bootstrap         the demo data seeder
```

## 3. The recovery engine

The engine is a **rule-based, explainable system**. It is not presented as a trained model, and
the codebase never claims otherwise.

```
RecoveryContext ──► RecoveryCostModel.evaluate()  ──► Map<RecoveryPath, RecoveryOption>
   (inspected           (documented coefficients)        (gross, costs, net, days,
    condition,                                             data quality, restrictions)
    weight, etc.)                    │
                                    ▼
                    RecoveryRankingStrategy.rank()
                    (weighted score: value, yield, speed,
                     circularity, confidence)
                                    │
                                    ▼
                    persisted RecoveryRecommendation
                    + explainable comparison view
```

`RecoveryRankingStrategy` is the seam. A learned model can be introduced by implementing that
interface and registering it under `returnloop.recovery.strategy`; nothing else in the application
needs to change.

The value model is:

```
netRecoveryValue = expectedResaleValue
                 - processingCost
                 - inspectionCost
                 - handlingCost
                 - transportationCost
                 - partnerFees
```

## 4. Route optimisation

`RouteOptimizer` is an interface with a single production implementation,
`NearestNeighborRouteOptimizer`:

1. sort pickups by priority, then by distance from the depot;
2. assign each pickup to the vehicle already carrying the most work that still has capacity
   (consolidation, not spread);
3. order each vehicle's stops with a nearest-neighbour sweep;
4. improve the ordering with a **2-opt** pass that reverses segments when that shortens the tour;
5. return to the depot.

Distances are great-circle (haversine) estimates from `GeoDistanceService`. They are labelled as
estimates everywhere in the API and the UI — the platform never presents them as live navigation
data.

## 5. Events and asynchronous workflows

Domain events are transport-agnostic records carrying a stable `eventId`.

* **In-process (default).** `InProcessDomainEventPublisher` dispatches to handlers synchronously
  and de-duplicates by `eventId`, so a replayed event never produces a second side effect.
* **Kafka (opt-in).** Set `returnloop.messaging.kafka.enabled=true` to publish to
  `returnloop.events` instead. `KafkaDomainEventConsumer` consumes with `@RetryableTopic`
  (3 attempts, exponential backoff) and routes exhausted messages to a dead-letter topic. The
  consumer de-duplicates by `eventId` as well.

Events are raised for: `return.created`, `return.status-changed`, `inspection.completed`,
`recovery.recommended`, `recovery.decision`, `pickup.scheduled`, `pickup.status-changed`,
`partner.job-assigned`, `partner.job-status-changed`, `route.optimized`.

## 6. Security model

| Concern | Mechanism |
| --- | --- |
| Authentication | Stateless JWT (HS256), 8 h default TTL, `jjwt` |
| Password storage | BCrypt, cost 12 |
| Authorisation | `Role` enum + `@EnableMethodSecurity`; the current user's role is embedded in the token |
| Tenant isolation | every record carries `organization_id`; the organisation comes from the signed token, never from the request body |
| Input validation | Jakarta Bean Validation on every request DTO |
| Error handling | one `ApiErrorResponse` envelope; stack traces are never returned |
| Secrets | environment variables only; the JWT secret must be ≥ 32 bytes or the app refuses to start |
| CORS | explicit origin allow-list from configuration |
| Uploads | 10 MB cap; the content type and size are validated server-side |
| Concurrency | JPA `@Version` optimistic locking on inventory and all entities |
| Idempotency | `Idempotency-Key` header on creation endpoints, fingerprinted with SHA-256 |

## 7. Data model

See [`erd.md`](./erd.md) for the entity relationship diagram. The short version:

* `organizations` is the tenant root; 22 tables reference it with `ON DELETE CASCADE`.
* Money is `NUMERIC(14,2)`, weights `NUMERIC(10,3)`, coordinates `NUMERIC(10,7)`.
* Identifiers are application-assigned UUIDv4 so they are known before the row is flushed.
* The `idempotency_records` table has a unique index on `(organization_id, idempotency_key)`.
* `inventory_items` has a check constraint preventing negative or over-reserved stock.

## 8. Deliberate limitations

* The operations map is a **simulated projection** of stored coordinates. Set
  `RETURNLOOP_MAP_API_KEY` to swap in a real tile provider.
* Route distances and durations are estimates, not live navigation.
* Environmental figures are modelled from weights and published category factors, and are labelled
  as estimates wherever they appear.
* The recovery engine is rule-based; partner performance metrics are derived from completed jobs
  and are blank until a partner has actually done work.

# ReturnLoop — REST API

Base URL: `http://localhost:8080/api/v1`
Interactive documentation: **http://localhost:8080/swagger-ui.html** (OpenAPI 3, generated from the
code by springdoc).

## Conventions

**Authentication.** All endpoints except `/api/v1/auth/**` require a bearer token.

```
Authorization: Bearer <accessToken>
```

Obtain one with `POST /api/v1/auth/login`. Tokens are JWT (HS256) and carry the user's
organisation and role.

**Envelope.** Successful single-resource responses are wrapped:

```json
{ "data": { ... }, "timestamp": "2026-10-22T09:15:00Z" }
```

Paginated responses are unwrapped and use a page envelope:

```json
{
  "content": [ ... ],
  "page": 0, "size": 25, "totalElements": 128,
  "totalPages": 6, "first": true, "last": false
}
```

**Errors.** Every failure uses one envelope:

```json
{
  "timestamp": "2026-10-22T09:15:00Z",
  "status": 422,
  "code": "CAPACITY_EXCEEDED",
  "message": "Assigning this pickup would exceed the capacity of Sprinter 12 (140 units)",
  "path": "/api/v1/logistics/pickups/…/schedule",
  "fieldErrors": { "quantity": ["must be at least 1"] },
  "details": null,
  "traceId": "b3f0…"
}
```

| Code | Status | Meaning |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | Request validation failed |
| `INVALID_STATE_TRANSITION` | 409 | The lifecycle step is not allowed from the current state |
| `RESOURCE_NOT_FOUND` | 404 | No such record in this organisation |
| `RESOURCE_ALREADY_EXISTS` | 409 | Duplicate SKU, email or identifier |
| `UNAUTHENTICATED` | 401 | Missing or invalid token |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `FORBIDDEN` | 403 | The role does not permit the action |
| `CROSS_TENANT_ACCESS` | 403 | The record belongs to another organisation |
| `CAPACITY_EXCEEDED` | 422 | The operation would break a capacity constraint |
| `DUPLICATE_ASSIGNMENT` | 409 | These goods already have an active job |
| `IDEMPOTENCY_KEY_REUSED` | 409 | The key was used with a different payload |
| `CONCURRENT_MODIFICATION` | 409 | Another user changed the record first |
| `INTERNAL_ERROR` | 500 | Unexpected failure (details are logged, never returned) |

**Idempotency.** Creation endpoints accept an optional `Idempotency-Key` header. Replaying the same
key with the same payload returns the original response; replaying it with a different payload is
rejected with `IDEMPOTENCY_KEY_REUSED`.

**Pagination.** List endpoints take `page` (0-based) and `size` (max 200) and a `sort` parameter
such as `createdAt,desc`.

---

## Authentication

### `POST /auth/login`

```json
{ "email": "ops@northwind-retail.example", "password": "ReturnLoop!2026" }
```

```json
{
  "data": {
    "accessToken": "eyJhbGciOi…",
    "tokenType": "Bearer",
    "expiresIn": 28800,
    "user": {
      "id": "…", "email": "ops@northwind-retail.example",
      "fullName": "Amara Okafor", "role": "ADMIN",
      "jobTitle": "Head of Reverse Operations", "organizationId": "…"
    }
  },
  "timestamp": "2026-10-22T09:15:00Z"
}
```

### `GET /auth/me`

The current user's profile.

---

## Catalogue

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/products` | Search by `search`, `category`, `page`, `size`, `sort` |
| `GET` | `/products/{id}` | One product |
| `POST` | `/products` | Create a product |
| `PUT` | `/products/{id}` | Update a product |

---

## Returns

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/returns` | Search by `search`, `status` (repeatable), `reason`, `from`, `to`, `page`, `size`, `sort` |
| `GET` | `/returns/{id}` | Detail with items and the lifecycle timeline |
| `POST` | `/returns` | Create a return with one or more items. Honours `Idempotency-Key` |
| `POST` | `/returns/{id}/items` | Add an item to an open return |
| `POST` | `/returns/{id}/status` | Advance the lifecycle |
| `POST` | `/returns/{id}/cancel` | Cancel with a recorded reason |
| `POST` | `/returns/{id}/warehouse` | Assign the receiving warehouse |
| `POST` | `/returns/{id}/estimated-value` | Record the estimated recovery value |
| `POST` | `/returns/{id}/realized-value` | Record the realised recovery value |

**Create a return**

```json
{
  "reason": "DEFECTIVE",
  "customerName": "Jordan Alvarez",
  "sourceReference": "ORD-88213",
  "originAddress": "220 Riverside Dr, Newark, NJ",
  "originLatitude": 40.7357,
  "originLongitude": -74.1724,
  "items": [
    { "sku": "NWR-PH-0142", "quantity": 1, "condition": "GOOD" },
    { "sku": "NWR-AU-0221", "quantity": 2, "condition": "LIKE_NEW" }
  ]
}
```

**Advance the lifecycle**

```json
{ "status": "PICKUP_SCHEDULED", "reason": "Collection booked for Tuesday" }
```

Legal transitions are enforced server-side:

| From | To |
| --- | --- |
| `RETURN_CREATED` | `PICKUP_SCHEDULED`, `CANCELLED` |
| `PICKUP_SCHEDULED` | `COLLECTED`, `PICKUP_SCHEDULED`, `CANCELLED` |
| `COLLECTED` | `WAREHOUSE_RECEIVED`, `CANCELLED` |
| `WAREHOUSE_RECEIVED` | `INSPECTED`, `CANCELLED` |
| `INSPECTED` | `RECOVERY_DECISION`, `CANCELLED` |
| `RECOVERY_DECISION` | `PARTNER_ASSIGNED`, `RECOVERY_DECISION`, `CANCELLED` |
| `PARTNER_ASSIGNED` | `PROCESSING`, `CANCELLED` |
| `PROCESSING` | `RECOVERED`, `CANCELLED` |
| `RECOVERED` | `COMPLETED` |
| `COMPLETED`, `CANCELLED` | — (terminal) |

---

## Inventory

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/inventory` | Search by `search`, `status`, `category`, `warehouseId` |
| `GET` | `/inventory/{id}` | One item with its movement history |
| `GET` | `/inventory/aging?thresholdDays=30` | Stock held beyond the threshold |
| `POST` | `/inventory/receive` | Book goods into a warehouse |
| `POST` | `/inventory/{id}/reserve` | Reserve stock for a job |
| `POST` | `/inventory/{id}/release` | Release a reservation |
| `POST` | `/inventory/{id}/dispatch` | Dispatch goods to a partner |
| `POST` | `/inventory/{id}/adjust` | Adjust the quantity; negative results are rejected |

---

## Inspections

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/inspections` | Search by `status`, `returnId` |
| `GET` | `/inspections/{id}` | One inspection with its attachments |
| `POST` | `/inspections/items/{returnItemId}` | Open an inspection for a return item |
| `POST` | `/inspections/{id}/record` | Record findings |
| `POST` | `/inspections/{id}/complete` | Complete and lock the inspection |
| `POST` | `/inspections/{id}/attachments` | Attach a photo or document |

Statuses: `AWAITING_INSPECTION`, `IN_PROGRESS`, `PASSED`, `DAMAGED`, `INCOMPLETE`, `UNSAFE`.
A completed inspection rejects further edits.

---

## Recovery engine

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/recovery/returns/{returnId}/options` | Preview the options without persisting |
| `POST` | `/recovery/returns/{returnId}/recommend` | Calculate and persist a recommendation |
| `GET` | `/recovery/returns/{returnId}/recommendations` | Stored recommendations |
| `POST` | `/recovery/recommendations/{id}/decision` | Approve or reject with a reason |

**Decision**

```json
{ "approve": true, "reason": "Repair cost confirmed with the partner" }
```

A rejection requires a reason. The engine is rule-based and reports the strategy it used
(`rule-based-explainable`); it is not a trained model.

---

## Partners and jobs

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/partners` | Search by `search`, `type`, `category`, `region`, `availableOnly` |
| `GET` | `/partners/{id}` | One partner with their recent jobs |
| `GET` | `/partners/match?path=REPAIR&category=ELECTRONICS` | Partners able to perform a path, best first |
| `POST` | `/partners/jobs` | Assign a recovery job to a partner |
| `GET` | `/partner-jobs` | Search by `search`, `status`, `partnerId`, `path` |
| `GET` | `/partner-jobs/{id}` | One job with its status history |
| `POST` | `/partner-jobs/{id}/status` | Advance the job |

Job lifecycle:

```
PROPOSED → ASSIGNED → ACCEPTED → PROCESSING → AWAITING_REVIEW → COMPLETED
                ↘ REJECTED          ↘ FAILED
```

Assigning a second active job to the same inventory item is rejected with `DUPLICATE_ASSIGNMENT`.

---

## Logistics

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/logistics/pickups` | Search by `status`, `date`, `vehicleId` |
| `GET` | `/logistics/pickups/{id}` | One pickup |
| `POST` | `/logistics/pickups` | Create and schedule a pickup |
| `POST` | `/logistics/pickups/{id}/schedule` | Assign a date, vehicle and driver |
| `POST` | `/logistics/pickups/{id}/status` | Advance the pickup |
| `POST` | `/logistics/pickups/{id}/reassign` | Move it to another vehicle or driver |
| `POST` | `/logistics/routes/optimize?date=2026-10-22` | Plan routes for a date |
| `GET` | `/logistics/routes` | Search by `status`, `date` |
| `GET` | `/logistics/routes/{id}` | One route with its stops |
| `POST` | `/logistics/routes/{id}/status` | Update the route status |
| `POST` | `/logistics/routes/{routeId}/stops/{stopId}/complete` | Complete a stop with evidence |
| `GET` | `/logistics/locations` | Warehouses and partner facilities |
| `GET` | `/logistics/vehicles` | The fleet |
| `GET` | `/logistics/drivers` | Available drivers |

Route responses carry `estimatedDistances: true` — the distances are great-circle estimates, not
live navigation data.

---

## Analytics

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/analytics/dashboard?from=&to=` | KPIs, trend, recovery distribution, map markers, recent returns |
| `GET` | `/analytics/reports?from=&to=` | The full reporting view |
| `GET` | `/analytics/trends?from=&to=` | The daily series alone |
| `GET` | `/analytics/environmental?from=&to=` | Estimated impact plus the methodology |

---

## Notifications, audit, organisation, users

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/notifications` | The current user's notifications |
| `GET` | `/notifications/unread-count` | Badge count |
| `POST` | `/notifications/{id}/read` | Mark one read |
| `POST` | `/notifications/read-all` | Mark all read |
| `GET` | `/audit?action=&entityType=` | The audit trail |
| `GET` | `/organizations/current` | The current tenant |
| `PUT` | `/organizations/current` | Update tenant settings |
| `GET` | `/users` | The organisation's users |
| `POST` | `/users` | Create a user |
| `PUT` | `/users/{id}` | Update a user |
| `POST` | `/users/{id}/deactivate` | Deactivate a user |

---

## CSV import

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/imports/returns` | Import returns (`multipart/form-data`, field `file`) |
| `POST` | `/imports/inventory` | Import unsold inventory |

Expected columns for a returns import:

```
return_number,reason,sku,quantity,condition,customer_name,source_reference
```

**Import report**

```json
{
  "data": {
    "type": "returns",
    "rowsRead": 3,
    "rowsImported": 1,
    "importedReferences": ["RET-2026-90001"],
    "errors": [
      { "line": 4, "returnNumber": "RET-2026-90002", "message": "No product is registered with SKU NWR-XXX-0000" }
    ],
    "notes": "Skus are matched against the product catalogue. Rows with an unknown SKU or invalid values are reported and skipped."
  },
  "timestamp": "2026-10-22T09:15:00Z"
}
```

Rows sharing a `return_number` are grouped into a single return. Valid rows are imported; invalid
rows are reported and skipped.

---

## Health and metrics

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/actuator/health` | Liveness and readiness, with component detail when authorised |
| `GET` | `/actuator/info` | Build information |
| `GET` | `/actuator/metrics` | Micrometer metrics |
| `GET` | `/actuator/prometheus` | Prometheus scrape endpoint |

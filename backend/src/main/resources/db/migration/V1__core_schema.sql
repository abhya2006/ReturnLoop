-- ReturnLoop core schema.
-- Money is NUMERIC(14,2); ids are UUID; timestamps are timestamptz in UTC.

CREATE TABLE organizations (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    name                  VARCHAR(160)     NOT NULL,
    slug                  VARCHAR(80)      NOT NULL,
    contact_email         VARCHAR(255),
    country               VARCHAR(80),
    default_currency      VARCHAR(3)       NOT NULL DEFAULT 'USD',
    active                BOOLEAN          NOT NULL DEFAULT TRUE,
    holding_threshold_days INTEGER         NOT NULL DEFAULT 30
);
CREATE UNIQUE INDEX uq_organizations_slug ON organizations (slug);

CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    email                 VARCHAR(255)     NOT NULL,
    password_hash         VARCHAR(120)     NOT NULL,
    full_name             VARCHAR(160)     NOT NULL,
    role                  VARCHAR(32)      NOT NULL,
    job_title             VARCHAR(120),
    active                BOOLEAN          NOT NULL DEFAULT TRUE,
    last_login_at         TIMESTAMPTZ,
    partner_id            UUID
);
CREATE UNIQUE INDEX uq_users_org_email ON users (organization_id, email);
CREATE INDEX idx_users_org ON users (organization_id);
CREATE INDEX idx_users_partner ON users (partner_id);

CREATE TABLE products (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    sku                   VARCHAR(64)      NOT NULL,
    name                  VARCHAR(240)     NOT NULL,
    brand                 VARCHAR(120),
    model                 VARCHAR(120),
    category              VARCHAR(40)      NOT NULL,
    description           VARCHAR(1000),
    image_url             VARCHAR(500),
    weight_kg             NUMERIC(10, 3),
    original_price        NUMERIC(14, 2),
    resale_value          NUMERIC(14, 2),
    active                BOOLEAN          NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_products_org_sku ON products (organization_id, sku);
CREATE INDEX idx_products_org ON products (organization_id);
CREATE INDEX idx_products_category ON products (organization_id, category);
CREATE INDEX idx_products_name ON products (name);

CREATE TABLE locations (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name                  VARCHAR(200)     NOT NULL,
    location_type         VARCHAR(40)      NOT NULL,
    address               VARCHAR(500),
    latitude              NUMERIC(10, 7)   NOT NULL,
    longitude             NUMERIC(10, 7)   NOT NULL,
    region                VARCHAR(80),
    capacity_units        INTEGER,
    active                BOOLEAN          NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_locations_org_type ON locations (organization_id, location_type);

CREATE TABLE vehicles (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name                  VARCHAR(120)     NOT NULL,
    registration          VARCHAR(40)      NOT NULL,
    vehicle_type          VARCHAR(60),
    capacity_units        INTEGER          NOT NULL DEFAULT 50,
    capacity_kg           NUMERIC(10, 2),
    home_location_id      UUID REFERENCES locations (id),
    active                BOOLEAN          NOT NULL DEFAULT TRUE,
    notes                 VARCHAR(500)
);
CREATE INDEX idx_vehicles_org_active ON vehicles (organization_id, active);

CREATE TABLE drivers (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name                  VARCHAR(160)     NOT NULL,
    phone                 VARCHAR(40),
    email                 VARCHAR(255),
    license_number        VARCHAR(60),
    active                BOOLEAN          NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_drivers_org_active ON drivers (organization_id, active);

CREATE TABLE return_requests (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    return_number         VARCHAR(40)      NOT NULL,
    status                VARCHAR(40)      NOT NULL,
    reason                VARCHAR(40)      NOT NULL,
    source_reference      VARCHAR(120),
    customer_name         VARCHAR(160),
    customer_email        VARCHAR(255),
    origin_address        VARCHAR(500),
    origin_latitude       NUMERIC(10, 7),
    origin_longitude      NUMERIC(10, 7),
    warehouse_id          UUID REFERENCES locations (id),
    pickup_id             UUID,
    notes                 VARCHAR(2000),
    estimated_value       NUMERIC(14, 2),
    realized_value        NUMERIC(14, 2),
    item_count            INTEGER          NOT NULL DEFAULT 0,
    status_changed_at     TIMESTAMPTZ      NOT NULL,
    cancelled_reason      VARCHAR(500)
);
CREATE UNIQUE INDEX uq_returns_org_number ON return_requests (organization_id, return_number);
CREATE INDEX idx_returns_org_status ON return_requests (organization_id, status);
CREATE INDEX idx_returns_org_created ON return_requests (organization_id, created_at);
CREATE INDEX idx_returns_reference ON return_requests (organization_id, source_reference);

CREATE TABLE return_items (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    return_id             UUID             NOT NULL REFERENCES return_requests (id) ON DELETE CASCADE,
    product_id            UUID             NOT NULL REFERENCES products (id),
    sku                   VARCHAR(64)      NOT NULL,
    product_name          VARCHAR(240)     NOT NULL,
    quantity              INTEGER          NOT NULL DEFAULT 1,
    condition             VARCHAR(40)      NOT NULL,
    serial_number         VARCHAR(120),
    accessories_complete  BOOLEAN          NOT NULL DEFAULT TRUE,
    estimated_value       NUMERIC(14, 2),
    notes                 VARCHAR(1000)
);
CREATE INDEX idx_return_items_return ON return_items (return_id);
CREATE INDEX idx_return_items_product ON return_items (organization_id, product_id);

CREATE TABLE return_events (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    return_id             UUID             NOT NULL REFERENCES return_requests (id) ON DELETE CASCADE,
    event_type            VARCHAR(60)      NOT NULL,
    from_status           VARCHAR(40),
    to_status             VARCHAR(40),
    description           VARCHAR(1000)    NOT NULL,
    actor                 VARCHAR(160),
    actor_role            VARCHAR(40)
);
CREATE INDEX idx_return_events_return ON return_events (return_id, created_at);

CREATE TABLE inventory_items (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    sku                   VARCHAR(64)      NOT NULL,
    product_id            UUID REFERENCES products (id),
    product_name          VARCHAR(240)     NOT NULL,
    category              VARCHAR(40),
    quantity              INTEGER          NOT NULL DEFAULT 0,
    reserved_quantity     INTEGER          NOT NULL DEFAULT 0,
    status                VARCHAR(40)      NOT NULL,
    condition             VARCHAR(40)      NOT NULL,
    warehouse_id          UUID REFERENCES locations (id),
    location_code         VARCHAR(40),
    estimated_value       NUMERIC(14, 2),
    unit_value            NUMERIC(14, 2),
    weight_kg             NUMERIC(10, 3),
    return_id             UUID REFERENCES return_requests (id),
    recovery_job_id       UUID,
    received_at           TIMESTAMPTZ      NOT NULL,
    dispatched_at         TIMESTAMPTZ,
    notes                 VARCHAR(1000),
    CONSTRAINT ck_inventory_non_negative CHECK (quantity >= 0 AND reserved_quantity >= 0
        AND reserved_quantity <= quantity)
);
CREATE INDEX idx_inventory_org_status ON inventory_items (organization_id, status);
CREATE INDEX idx_inventory_org_sku ON inventory_items (organization_id, sku);
CREATE INDEX idx_inventory_warehouse ON inventory_items (warehouse_id);

CREATE TABLE inventory_movements (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    inventory_item_id     UUID             NOT NULL REFERENCES inventory_items (id) ON DELETE CASCADE,
    movement_type         VARCHAR(40)      NOT NULL,
    quantity              INTEGER          NOT NULL,
    from_status           VARCHAR(40),
    to_status             VARCHAR(40),
    value_delta           NUMERIC(14, 2),
    reference_type        VARCHAR(60),
    reference_id          UUID,
    note                  VARCHAR(500),
    actor                 VARCHAR(160)
);
CREATE INDEX idx_movements_item ON inventory_movements (inventory_item_id, created_at);

CREATE TABLE inspections (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    return_id             UUID             NOT NULL REFERENCES return_requests (id) ON DELETE CASCADE,
    return_item_id        UUID             NOT NULL REFERENCES return_items (id) ON DELETE CASCADE,
    sku                   VARCHAR(64),
    product_name          VARCHAR(240),
    status                VARCHAR(40)      NOT NULL,
    packaging_condition   VARCHAR(40),
    functionality_result  VARCHAR(40),
    functional            BOOLEAN          NOT NULL DEFAULT FALSE,
    accessories_complete  BOOLEAN          NOT NULL DEFAULT TRUE,
    damage_description    VARCHAR(1000),
    missing_accessories   VARCHAR(500),
    estimated_repair_cost NUMERIC(14, 2),
    notes                 VARCHAR(2000),
    inspected_by          VARCHAR(160),
    inspected_at          TIMESTAMPTZ,
    flagged_unsafe        BOOLEAN          NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_inspections_return_item ON inspections (return_item_id);
CREATE INDEX idx_inspections_org_status ON inspections (organization_id, status);

CREATE TABLE inspection_attachments (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    inspection_id         UUID             NOT NULL REFERENCES inspections (id) ON DELETE CASCADE,
    file_name             VARCHAR(255)     NOT NULL,
    content_type          VARCHAR(120)     NOT NULL,
    storage_path          VARCHAR(500)     NOT NULL,
    size_bytes            BIGINT           NOT NULL,
    caption               VARCHAR(500)
);
CREATE INDEX idx_attachments_inspection ON inspection_attachments (inspection_id);

CREATE TABLE recovery_recommendations (
    id                        UUID PRIMARY KEY,
    created_at                TIMESTAMPTZ      NOT NULL,
    updated_at                TIMESTAMPTZ      NOT NULL,
    version                   BIGINT           NOT NULL DEFAULT 0,
    organization_id           UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    return_id                 UUID             NOT NULL REFERENCES return_requests (id) ON DELETE CASCADE,
    return_item_id            UUID             NOT NULL REFERENCES return_items (id) ON DELETE CASCADE,
    product_id                UUID REFERENCES products (id),
    sku                       VARCHAR(64),
    recommended_path          VARCHAR(40)      NOT NULL,
    recommended_net_value     NUMERIC(14, 2),
    options_json              TEXT,
    strategy                  VARCHAR(60)      NOT NULL,
    approved                  BOOLEAN          NOT NULL DEFAULT FALSE,
    rejected                  BOOLEAN          NOT NULL DEFAULT FALSE,
    decision_reason           VARCHAR(1000),
    decided_by                VARCHAR(160),
    decided_at                TIMESTAMPTZ,
    estimated_processing_days INTEGER
);
CREATE INDEX idx_recovery_return_item ON recovery_recommendations (return_id, return_item_id);
CREATE INDEX idx_recovery_org_created ON recovery_recommendations (organization_id, created_at);

CREATE TABLE partners (
    id                        UUID PRIMARY KEY,
    created_at                TIMESTAMPTZ      NOT NULL,
    updated_at                TIMESTAMPTZ      NOT NULL,
    version                   BIGINT           NOT NULL DEFAULT 0,
    organization_id           UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name                      VARCHAR(200)     NOT NULL,
    partner_type              VARCHAR(40)      NOT NULL,
    description               VARCHAR(2000),
    contact_name              VARCHAR(160),
    contact_email             VARCHAR(255),
    contact_phone             VARCHAR(40),
    serviceable_regions       VARCHAR(1000),
    supported_categories      VARCHAR(1000),
    processing_capacity_per_day INTEGER       NOT NULL DEFAULT 50,
    currently_available       BOOLEAN          NOT NULL DEFAULT TRUE,
    fee_per_unit              NUMERIC(14, 2),
    fee_percentage            NUMERIC(5, 2),
    acceptance_criteria       VARCHAR(2000),
    latitude                  NUMERIC(10, 7),
    longitude                 NUMERIC(10, 7),
    address                   VARCHAR(500),
    verified                  BOOLEAN          NOT NULL DEFAULT FALSE,
    verified_at               TIMESTAMPTZ,
    completed_jobs            INTEGER          NOT NULL DEFAULT 0,
    failed_jobs               INTEGER          NOT NULL DEFAULT 0,
    average_processing_days   NUMERIC(8, 2),
    acceptance_rate           NUMERIC(5, 2),
    on_time_rate              NUMERIC(5, 2)
);
CREATE INDEX idx_partners_org_type ON partners (organization_id, partner_type);
CREATE INDEX idx_partners_org_name ON partners (organization_id, name);

CREATE TABLE recovery_jobs (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    job_number            VARCHAR(40)      NOT NULL,
    return_id             UUID REFERENCES return_requests (id),
    return_item_id        UUID REFERENCES return_items (id),
    inventory_item_id     UUID REFERENCES inventory_items (id),
    sku                   VARCHAR(64),
    product_name          VARCHAR(240),
    quantity              INTEGER          NOT NULL DEFAULT 1,
    recovery_path         VARCHAR(40)      NOT NULL,
    status                VARCHAR(40)      NOT NULL,
    partner_id            UUID REFERENCES partners (id),
    partner_name          VARCHAR(200),
    expected_recovery_value NUMERIC(14, 2),
    cost_estimate         NUMERIC(14, 2),
    realized_value        NUMERIC(14, 2),
    service_deadline      DATE,
    assigned_at           TIMESTAMPTZ,
    accepted_at           TIMESTAMPTZ,
    started_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    evidence_note         VARCHAR(2000),
    exception_note        VARCHAR(2000),
    rejection_reason      VARCHAR(1000),
    progress_note         VARCHAR(1000)
);
CREATE UNIQUE INDEX uq_jobs_org_number ON recovery_jobs (organization_id, job_number);
CREATE INDEX idx_jobs_org_status ON recovery_jobs (organization_id, status);
CREATE INDEX idx_jobs_partner ON recovery_jobs (partner_id, status);
CREATE INDEX idx_jobs_return ON recovery_jobs (return_id);
CREATE INDEX idx_jobs_inventory ON recovery_jobs (inventory_item_id);

CREATE TABLE recovery_job_events (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    job_id                UUID             NOT NULL REFERENCES recovery_jobs (id) ON DELETE CASCADE,
    event_type            VARCHAR(60)      NOT NULL,
    from_status           VARCHAR(40),
    to_status             VARCHAR(40),
    description           VARCHAR(1000)    NOT NULL,
    actor                 VARCHAR(160)
);
CREATE INDEX idx_job_events_job ON recovery_job_events (job_id, created_at);

CREATE TABLE pickup_jobs (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    pickup_number         VARCHAR(40)      NOT NULL,
    return_id             UUID REFERENCES return_requests (id),
    status                VARCHAR(40)      NOT NULL,
    origin_address        VARCHAR(500),
    origin_latitude       NUMERIC(10, 7),
    origin_longitude      NUMERIC(10, 7),
    destination_location_id UUID REFERENCES locations (id),
    destination_name      VARCHAR(200),
    scheduled_date        DATE,
    window_start          TIME,
    window_end            TIME,
    priority              INTEGER          NOT NULL DEFAULT 5,
    unit_count            INTEGER          NOT NULL DEFAULT 1,
    total_weight_kg       NUMERIC(10, 3),
    vehicle_id            UUID REFERENCES vehicles (id),
    driver_id             UUID REFERENCES drivers (id),
    route_id              UUID,
    stop_sequence         INTEGER,
    estimated_distance_km NUMERIC(10, 3),
    estimated_duration_minutes INTEGER,
    estimated_cost        NUMERIC(14, 2),
    completed_at          TIMESTAMPTZ,
    failure_reason        VARCHAR(1000),
    evidence_note         VARCHAR(2000),
    notes                 VARCHAR(1000)
);
CREATE UNIQUE INDEX uq_pickups_org_number ON pickup_jobs (organization_id, pickup_number);
CREATE INDEX idx_pickups_org_status ON pickup_jobs (organization_id, status);
CREATE INDEX idx_pickups_date ON pickup_jobs (scheduled_date, status);

CREATE TABLE routes (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    route_number          VARCHAR(40)      NOT NULL,
    status                VARCHAR(40)      NOT NULL,
    route_date            DATE,
    vehicle_id            UUID REFERENCES vehicles (id),
    vehicle_name          VARCHAR(120),
    driver_id             UUID REFERENCES drivers (id),
    driver_name           VARCHAR(160),
    origin_location_id    UUID REFERENCES locations (id),
    origin_name           VARCHAR(200),
    stop_count            INTEGER          NOT NULL DEFAULT 0,
    total_units           INTEGER          NOT NULL DEFAULT 0,
    total_weight_kg       NUMERIC(10, 3),
    estimated_distance_km NUMERIC(10, 3),
    estimated_duration_minutes INTEGER,
    estimated_cost        NUMERIC(14, 2),
    algorithm             VARCHAR(60)      NOT NULL,
    notes                 VARCHAR(1000)
);
CREATE UNIQUE INDEX uq_routes_org_number ON routes (organization_id, route_number);
CREATE INDEX idx_routes_org_date ON routes (organization_id, route_date);
CREATE INDEX idx_routes_vehicle ON routes (vehicle_id);

CREATE TABLE route_stops (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    route_id              UUID             NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    pickup_job_id         UUID REFERENCES pickup_jobs (id),
    sequence              INTEGER          NOT NULL,
    stop_type             VARCHAR(40)      NOT NULL,
    location_name         VARCHAR(200)     NOT NULL,
    address               VARCHAR(500),
    latitude              NUMERIC(10, 7),
    longitude             NUMERIC(10, 7),
    unit_count            INTEGER          NOT NULL DEFAULT 0,
    leg_distance_km       NUMERIC(10, 3),
    leg_duration_minutes  INTEGER,
    completed             BOOLEAN          NOT NULL DEFAULT FALSE,
    completed_at          TIMESTAMPTZ,
    note                  VARCHAR(500)
);
CREATE INDEX idx_route_stops_route ON route_stops (route_id, sequence);

CREATE TABLE notifications (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id               UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title                 VARCHAR(240)     NOT NULL,
    body                  VARCHAR(2000),
    category              VARCHAR(60)      NOT NULL DEFAULT 'GENERAL',
    severity              VARCHAR(20)      NOT NULL DEFAULT 'INFO',
    link                  VARCHAR(500),
    reference_type        VARCHAR(60),
    reference_id          UUID,
    read                  BOOLEAN          NOT NULL DEFAULT FALSE,
    read_at               TIMESTAMPTZ
);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read);
CREATE INDEX idx_notifications_org ON notifications (organization_id, created_at);

CREATE TABLE audit_events (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    actor                 VARCHAR(160)     NOT NULL,
    actor_role            VARCHAR(40),
    action                VARCHAR(80)      NOT NULL,
    entity_type           VARCHAR(60)      NOT NULL,
    entity_id             UUID,
    summary               VARCHAR(1000),
    details               TEXT,
    ip_address            VARCHAR(60)
);
CREATE INDEX idx_audit_org_created ON audit_events (organization_id, created_at);
CREATE INDEX idx_audit_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_events (actor);

CREATE TABLE idempotency_records (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    version               BIGINT           NOT NULL DEFAULT 0,
    organization_id       UUID             NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    idempotency_key       VARCHAR(128)     NOT NULL,
    operation             VARCHAR(64)      NOT NULL,
    request_fingerprint   VARCHAR(64)      NOT NULL,
    response_body         TEXT,
    status_code           INTEGER,
    completed             BOOLEAN          NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uq_idempotency_org_key ON idempotency_records (organization_id, idempotency_key);
CREATE INDEX idx_idempotency_key ON idempotency_records (idempotency_key);

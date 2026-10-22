CREATE TABLE service_requests (
    id           BIGSERIAL PRIMARY KEY,
    service_id   BIGINT NOT NULL REFERENCES services(id),
    client_id    BIGINT NOT NULL REFERENCES users(id),
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP,
    notes        TEXT,
    total_price  NUMERIC(10,2) NOT NULL,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (status IN
        ('PENDING','ACCEPTED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

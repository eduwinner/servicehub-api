-- Fase 3: leva o banco ao schema-alvo entregue aos alunos (docs/alunos/servicehub_schema.sql).
-- Nunca edite migrações já aplicadas: corrige-se com uma nova versão.

-- ---------------------------------------------------------------- users
UPDATE users SET is_active = TRUE WHERE is_active IS NULL;
UPDATE users SET created_at = NOW() WHERE created_at IS NULL;
UPDATE users SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE users SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));

ALTER TABLE users
    ALTER COLUMN is_active  SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT chk_users_email_low CHECK (email = LOWER(email));

-- ---------------------------------------------------------------- services
ALTER TABLE services ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();
UPDATE services SET is_active = TRUE WHERE is_active IS NULL;
UPDATE services SET created_at = NOW() WHERE created_at IS NULL;

ALTER TABLE services
    ALTER COLUMN is_active  SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    DROP CONSTRAINT services_provider_id_fkey,
    ADD CONSTRAINT fk_services_provider FOREIGN KEY (provider_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_services_price CHECK (price > 0),
    ADD CONSTRAINT chk_services_category CHECK (category IN (
        'LIMPEZA', 'DIARISTA', 'ENCANAMENTO', 'ELETRICA', 'JARDINAGEM', 'PINTURA',
        'MARCENARIA', 'PEDREIRO', 'DEDETIZACAO', 'AR_CONDICIONADO', 'CHAVEIRO',
        'MONTAGEM_MOVEIS', 'MUDANCA', 'SUPORTE_TECNICO', 'MANUTENCAO_COMPUTADORES',
        'DESENVOLVIMENTO_SOFTWARE', 'REDES_E_INFRAESTRUTURA', 'SEGURANCA_DIGITAL',
        'RECUPERACAO_DE_DADOS', 'CONSULTORIA_TI', 'OUTROS'));

DROP INDEX IF EXISTS idx_services_category;
CREATE INDEX idx_services_category ON services (category) WHERE is_active;

-- ---------------------------------------------------------------- service_requests
UPDATE service_requests SET created_at = NOW() WHERE created_at IS NULL;
UPDATE service_requests SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE service_requests
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    DROP CONSTRAINT service_requests_service_id_fkey,
    DROP CONSTRAINT service_requests_client_id_fkey,
    ADD CONSTRAINT fk_requests_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_requests_client  FOREIGN KEY (client_id)  REFERENCES users (id)    ON DELETE RESTRICT,
    ADD CONSTRAINT chk_requests_price CHECK (total_price > 0);

CREATE INDEX idx_requests_service ON service_requests (service_id);
CREATE INDEX idx_requests_client  ON service_requests (client_id);
CREATE INDEX idx_requests_status  ON service_requests (status);

-- ---------------------------------------------------------------- reviews
UPDATE reviews SET created_at = NOW() WHERE created_at IS NULL;

ALTER TABLE reviews
    ALTER COLUMN created_at SET NOT NULL,
    DROP CONSTRAINT reviews_request_id_fkey,
    DROP CONSTRAINT reviews_reviewer_id_fkey,
    ADD CONSTRAINT fk_reviews_request  FOREIGN KEY (request_id)  REFERENCES service_requests (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)            ON DELETE RESTRICT;

CREATE INDEX idx_reviews_reviewer ON reviews (reviewer_id);

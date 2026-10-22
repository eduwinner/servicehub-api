ALTER TABLE services
    DROP CONSTRAINT services_provider_id_fkey,
    ADD CONSTRAINT services_provider_id_fkey
        FOREIGN KEY (provider_id) REFERENCES users(id)
        ON DELETE CASCADE;

CREATE INDEX idx_services_provider
    ON services(provider_id);
CREATE INDEX idx_services_category
    ON services(category);

-- Fase 2: papel do usuário (Aula 9: ADMIN, PROVIDER, CLIENT)
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CLIENT';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'PROVIDER', 'CLIENT'));

-- =============================================================================
--  ServiceHub API — Schema de referência (PostgreSQL 16)
--  Disciplina: Desenvolvimento de API Back-End — UniEVANGÉLICA — 2026/2
--  Documento de apoio: "Mapa de Construção da API" (PDF)
--
--  Este arquivo descreve o ESTADO FINAL do banco que a sua API deve ter.
--  Ele NÃO deve ser executado por cima do banco do seu projeto.
--
--  Como usar no seu projeto (Flyway + ddl-auto=validate):
--    1. Compare este schema com as migrações que você já tem.
--    2. Crie NOVAS migrações (V{n+1}__..., V{n+2}__...) que levem o seu banco
--       até este estado. Nunca edite uma migração já aplicada.
--    3. Ajuste as entidades JPA até o Hibernate validar sem erro.
--
--  ATENÇÃO ao migrar um banco que já tem dados:
--    • Antes de criar o CHECK (email = LOWER(email)), rode
--        SELECT LOWER(email), COUNT(*) FROM users GROUP BY LOWER(email) HAVING COUNT(*) > 1;
--      E-mails iguais exceto pelas maiúsculas viram duplicados ao normalizar e a UNIQUE falha.
--      Resolva os duplicados (mescle ou renomeie) ANTES da migração.
--    • Antes dos CHECKs de price, category e status, confira se não há valores fora da regra.
--    • Uma migração que falhou no meio deixa o Flyway marcado como failed: corrija o SQL e use
--      flyway repair (ou apague o banco de desenvolvimento) antes de tentar de novo.
--
--  Para conferir o schema isolado, num banco vazio:
--    docker run --rm -d --name schema-check -e POSTGRES_PASSWORD=secret -p 5499:5432 postgres:16-alpine
--    psql -h localhost -p 5499 -U postgres -f servicehub_schema.sql
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- users — pessoas que usam a plataforma (clientes, prestadores e administradores)
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id             BIGSERIAL     PRIMARY KEY,
    full_name      VARCHAR(100)  NOT NULL,
    email          VARCHAR(255)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,              -- hash BCrypt, NUNCA a senha
    role           VARCHAR(20)   NOT NULL DEFAULT 'CLIENT',
    phone          VARCHAR(20),
    bio            TEXT,
    avatar_url     VARCHAR(500),
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,  -- exclusão lógica (soft delete)
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_users_email      UNIQUE (email),
    CONSTRAINT chk_users_email_low CHECK (email = LOWER(email)),   -- e-mail sempre normalizado
    CONSTRAINT chk_users_role      CHECK (role IN ('ADMIN', 'PROVIDER', 'CLIENT'))
);

COMMENT ON TABLE  users               IS 'Usuários da plataforma. Papel único por usuário.';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt (60 caracteres). Nunca sai em respostas da API.';
COMMENT ON COLUMN users.is_active     IS 'FALSE = usuário excluído logicamente: não faz login e o JWT deixa de valer.';

-- -----------------------------------------------------------------------------
-- services — serviços oferecidos por um prestador (PROVIDER)
-- (a entidade Java costuma se chamar ServiceOffering para não colidir com @Service)
-- -----------------------------------------------------------------------------
CREATE TABLE services (
    id           BIGSERIAL      PRIMARY KEY,
    title        VARCHAR(150)   NOT NULL,
    description  TEXT,
    price        NUMERIC(10,2)  NOT NULL,
    category     VARCHAR(50)    NOT NULL,
    provider_id  BIGINT         NOT NULL,
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_services_provider  FOREIGN KEY (provider_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_services_price    CHECK (price > 0),
    CONSTRAINT chk_services_category CHECK (category IN (
        'LIMPEZA', 'DIARISTA', 'ENCANAMENTO', 'ELETRICA', 'JARDINAGEM', 'PINTURA',
        'MARCENARIA', 'PEDREIRO', 'DEDETIZACAO', 'AR_CONDICIONADO', 'CHAVEIRO',
        'MONTAGEM_MOVEIS', 'MUDANCA', 'SUPORTE_TECNICO', 'MANUTENCAO_COMPUTADORES',
        'DESENVOLVIMENTO_SOFTWARE', 'REDES_E_INFRAESTRUTURA', 'SEGURANCA_DIGITAL',
        'RECUPERACAO_DE_DADOS', 'CONSULTORIA_TI', 'OUTROS'))
);

CREATE INDEX idx_services_provider ON services (provider_id);
CREATE INDEX idx_services_category ON services (category) WHERE is_active;

COMMENT ON COLUMN services.category IS 'Mesmo conjunto do enum Java ServiceCategory (EnumType.STRING). Nova categoria = nova migração.';

-- -----------------------------------------------------------------------------
-- service_requests — ordens de serviço: um usuário contrata um serviço
-- -----------------------------------------------------------------------------
CREATE TABLE service_requests (
    id            BIGSERIAL      PRIMARY KEY,
    service_id    BIGINT         NOT NULL,
    client_id     BIGINT         NOT NULL,
    status        VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    scheduled_at  TIMESTAMP,
    notes         TEXT,
    total_price   NUMERIC(10,2)  NOT NULL,              -- cópia do preço no momento da contratação
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_requests_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE RESTRICT,
    CONSTRAINT fk_requests_client  FOREIGN KEY (client_id)  REFERENCES users (id)    ON DELETE RESTRICT,
    CONSTRAINT chk_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_requests_price  CHECK (total_price > 0)
);

CREATE INDEX idx_requests_service ON service_requests (service_id);
CREATE INDEX idx_requests_client  ON service_requests (client_id);
CREATE INDEX idx_requests_status  ON service_requests (status);

COMMENT ON COLUMN service_requests.total_price IS 'Snapshot de services.price na criação. Mudar o preço do serviço não altera ordens existentes.';
COMMENT ON COLUMN service_requests.status      IS 'Máquina de estados validada na camada de serviço; o CHECK só garante valores conhecidos.';

-- -----------------------------------------------------------------------------
-- reviews — avaliação de uma ordem concluída (no máximo uma por ordem)
-- -----------------------------------------------------------------------------
CREATE TABLE reviews (
    id           BIGSERIAL  PRIMARY KEY,
    request_id   BIGINT     NOT NULL,
    reviewer_id  BIGINT     NOT NULL,
    rating       SMALLINT   NOT NULL,
    comment      TEXT,
    created_at   TIMESTAMP  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_reviews_request   UNIQUE (request_id),
    CONSTRAINT fk_reviews_request   FOREIGN KEY (request_id)  REFERENCES service_requests (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewer  FOREIGN KEY (reviewer_id) REFERENCES users (id)            ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating   CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_reviewer ON reviews (reviewer_id);

COMMIT;

-- -----------------------------------------------------------------------------
-- Consultas úteis (não fazem parte do schema)
-- -----------------------------------------------------------------------------
-- Média de avaliações por serviço:
--   SELECT s.id, s.title, ROUND(AVG(r.rating), 1) AS media, COUNT(r.id) AS total
--     FROM services s
--     JOIN service_requests sr ON sr.service_id = s.id
--     JOIN reviews r           ON r.request_id  = sr.id
--    GROUP BY s.id, s.title;
--
-- Ordens ativas de um prestador (bloqueiam a exclusão do serviço/usuário):
--   SELECT sr.* FROM service_requests sr JOIN services s ON s.id = sr.service_id
--    WHERE s.provider_id = :providerId AND sr.status IN ('PENDING','ACCEPTED','IN_PROGRESS');

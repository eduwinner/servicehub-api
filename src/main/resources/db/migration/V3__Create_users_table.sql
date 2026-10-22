ALTER TABLE users
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN bio TEXT,
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

CREATE INDEX idx_users_email
    ON users(email);

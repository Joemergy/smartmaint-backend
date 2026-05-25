CREATE TABLE IF NOT EXISTS smartmaint.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    correo VARCHAR(150) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rt_token ON smartmaint.refresh_tokens (token);
CREATE INDEX idx_rt_correo ON smartmaint.refresh_tokens (correo);
CREATE INDEX idx_rt_expires ON smartmaint.refresh_tokens (expires_at);

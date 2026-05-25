CREATE TABLE IF NOT EXISTS smartmaint.roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

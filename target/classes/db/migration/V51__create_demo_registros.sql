CREATE TABLE IF NOT EXISTS smartmaint.demo_registros (
    id BIGSERIAL PRIMARY KEY,
    nombre_solicitante VARCHAR(255),
    empresa_solicitada VARCHAR(255),
    ip_solicitante VARCHAR(255),
    fecha_solicitud TIMESTAMP
);

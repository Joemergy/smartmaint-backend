CREATE TABLE IF NOT EXISTS smartmaint.notificaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    mensaje VARCHAR(255) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES smartmaint.usuarios(id)
);

CREATE INDEX idx_notificaciones_usuario_leido_fecha ON smartmaint.notificaciones (usuario_id, leido, fecha DESC);

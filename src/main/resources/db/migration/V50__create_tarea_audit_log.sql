CREATE TABLE IF NOT EXISTS smartmaint.tarea_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tarea_id BIGINT NOT NULL,
    actor_correo VARCHAR(150),
    accion VARCHAR(40) NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30),
    detalle VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tarea_audit_log_tarea FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id)
);

CREATE INDEX idx_tarea_audit_tarea_id ON smartmaint.tarea_audit_log (tarea_id);
CREATE INDEX idx_tarea_audit_creado_en ON smartmaint.tarea_audit_log (creado_en DESC);

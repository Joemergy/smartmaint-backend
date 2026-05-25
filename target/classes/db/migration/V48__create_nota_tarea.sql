CREATE TABLE IF NOT EXISTS smartmaint.nota_tarea (
    id BIGSERIAL PRIMARY KEY,
    tarea_id BIGINT NOT NULL,
    autor_id BIGINT,
    autor_nombre VARCHAR(150),
    texto TEXT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_nota_tarea_tarea FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id),
    CONSTRAINT fk_nota_tarea_autor FOREIGN KEY (autor_id) REFERENCES smartmaint.usuarios(id)
);

CREATE INDEX idx_nota_tarea_tarea_fecha ON smartmaint.nota_tarea (tarea_id, fecha_creacion ASC);

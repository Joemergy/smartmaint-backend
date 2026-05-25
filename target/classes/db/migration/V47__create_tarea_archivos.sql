CREATE TABLE IF NOT EXISTS smartmaint.tarea_archivos (
    tarea_id BIGINT NOT NULL,
    archivo VARCHAR(255),
    CONSTRAINT fk_tarea_archivos_tarea FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id)
);

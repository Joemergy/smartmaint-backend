CREATE TABLE IF NOT EXISTS smartmaint.tareas (
    id BIGSERIAL PRIMARY KEY,
    estado VARCHAR(20) NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    descripcion TEXT,
    nota_tecnica TEXT,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP,
    categoria VARCHAR(30),
    nombre_maquina VARCHAR(50) NOT NULL,
    id_maquina VARCHAR(255),
    ubicacion VARCHAR(50),
    id_colaborador VARCHAR(50),
    nombre_colaborador VARCHAR(100),
    correo_colaborador VARCHAR(100),
    grupal BOOLEAN,
    observaciones TEXT,
    prioridad VARCHAR(20),
    usuario_id BIGINT,
    CONSTRAINT fk_tareas_usuario FOREIGN KEY (usuario_id) REFERENCES smartmaint.usuarios(id)
);

CREATE INDEX idx_tareas_estado ON smartmaint.tareas (estado);
CREATE INDEX idx_tareas_correo_colaborador ON smartmaint.tareas (correo_colaborador);
CREATE INDEX idx_tareas_fecha_inicio ON smartmaint.tareas (fecha_inicio DESC);
CREATE INDEX idx_tareas_fecha_cierre ON smartmaint.tareas (fecha_cierre DESC);
CREATE INDEX idx_tareas_usuario_id ON smartmaint.tareas (usuario_id);

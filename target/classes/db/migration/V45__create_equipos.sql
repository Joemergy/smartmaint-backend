CREATE TABLE IF NOT EXISTS smartmaint.equipos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(30),
    ubicacion VARCHAR(50),
    categoria VARCHAR(30),
    descripcion VARCHAR(120),
    created_at TIMESTAMP DEFAULT NOW(),
    empresa_id BIGINT NOT NULL,
    CONSTRAINT fk_equipos_empresa FOREIGN KEY (empresa_id) REFERENCES smartmaint.empresas(id),
    CONSTRAINT uk_equipo_empresa_nombre UNIQUE (empresa_id, nombre)
);

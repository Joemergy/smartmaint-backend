CREATE TABLE IF NOT EXISTS smartmaint.usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    correo VARCHAR(50) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    rol_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_colaborador VARCHAR(50) NOT NULL UNIQUE,
    cargo VARCHAR(100) NOT NULL,
    area VARCHAR(100) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    fecha_ingreso DATE,
    direccion VARCHAR(255),
    foto_perfil VARCHAR(255),
    debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT FALSE,
    demo BOOLEAN NOT NULL DEFAULT FALSE,
    expira_en TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES smartmaint.roles(id),
    CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id) REFERENCES smartmaint.empresas(id)
);

CREATE INDEX idx_usuarios_empresa_rol ON smartmaint.usuarios (empresa_id, rol_id);
CREATE INDEX idx_usuarios_demo_expira_en ON smartmaint.usuarios (demo, expira_en);

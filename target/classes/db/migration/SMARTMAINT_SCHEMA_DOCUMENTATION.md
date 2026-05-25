# Documentación del esquema `smartmaint`

Este documento describe las tablas del esquema `smartmaint` usadas por el backend, con columnas, tipos de dato, restricciones y los índices asociados.

---

## 1. `smartmaint.usuarios`

- `id` BIGSERIAL PRIMARY KEY
- `nombre` VARCHAR(50) NOT NULL
- `correo` VARCHAR(50) NOT NULL UNIQUE
- `contrasena` VARCHAR(255) NOT NULL
- `rol_id` BIGINT NOT NULL
- `empresa_id` BIGINT NOT NULL
- `activo` BOOLEAN NOT NULL DEFAULT TRUE
- `id_colaborador` VARCHAR(50) NOT NULL UNIQUE
- `cargo` VARCHAR(100) NOT NULL
- `area` VARCHAR(100) NOT NULL
- `telefono` VARCHAR(30) NOT NULL
- `fecha_ingreso` DATE
- `direccion` VARCHAR(255)
- `foto_perfil` VARCHAR(255)
- `debe_cambiar_contrasena` BOOLEAN NOT NULL DEFAULT FALSE
- `demo` BOOLEAN DEFAULT FALSE
- `expira_en` TIMESTAMP
- `created_at` TIMESTAMP
- `updated_at` TIMESTAMP

### Relaciones
- `rol_id` → `smartmaint.roles(id)`
- `empresa_id` → `smartmaint.empresas(id)`

### Índices sugeridos
- `idx_usuarios_empresa_rol` (`empresa_id`, `rol_id`)
- `idx_usuarios_demo_expira_en` (`demo`, `expira_en`)

---

## 2. `smartmaint.roles`

- `id` BIGSERIAL PRIMARY KEY
- `nombre` VARCHAR(20) NOT NULL UNIQUE
- `created_at` TIMESTAMP
- `updated_at` TIMESTAMP

---

## 3. `smartmaint.empresas`

- `id` BIGSERIAL PRIMARY KEY
- `nombre` VARCHAR(100) NOT NULL UNIQUE
- `correo` VARCHAR(100) NOT NULL UNIQUE
- `sector` VARCHAR(50) NOT NULL
- `plan` VARCHAR(50) NOT NULL
- `created_at` TIMESTAMP
- `activa` BOOLEAN NOT NULL DEFAULT FALSE
- `token` VARCHAR(255) UNIQUE

### Notas
- `plan` se mapea con el enum Java `PlanEmpresa` y debe almacenarse como texto: `MENSUAL`, `ANUAL`, `DEMO`.

---

## 4. `smartmaint.equipos`

- `id` BIGSERIAL PRIMARY KEY
- `nombre` VARCHAR(50) NOT NULL
- `tipo` VARCHAR(30)
- `ubicacion` VARCHAR(50)
- `categoria` VARCHAR(30)
- `descripcion` VARCHAR(120)
- `created_at` TIMESTAMP
- `empresa_id` BIGINT NOT NULL

### Restricciones
- Unique constraint: `uk_equipo_empresa_nombre` sobre (`empresa_id`, `nombre`)

### Relaciones
- `empresa_id` → `smartmaint.empresas(id)`

---

## 5. `smartmaint.tareas`

- `id` BIGSERIAL PRIMARY KEY
- `estado` VARCHAR(20) NOT NULL
- `titulo` VARCHAR(100) NOT NULL
- `descripcion` TEXT
- `nota_tecnica` TEXT
- `fecha_inicio` TIMESTAMP NOT NULL
- `fecha_cierre` TIMESTAMP
- `categoria` VARCHAR(30)
- `nombre_maquina` VARCHAR(50) NOT NULL
- `id_maquina` VARCHAR(255)
- `ubicacion` VARCHAR(50)
- `id_colaborador` VARCHAR(50)
- `nombre_colaborador` VARCHAR(100)
- `correo_colaborador` VARCHAR(100)
- `grupal` BOOLEAN
- `observaciones` TEXT
- `prioridad` VARCHAR(20)
- `usuario_id` BIGINT

### Relaciones
- `usuario_id` → `smartmaint.usuarios(id)`

### Índices sugeridos
- `idx_tareas_estado` (`estado`)
- `idx_tareas_correo_colaborador` (`correo_colaborador`)
- `idx_tareas_fecha_inicio` (`fecha_inicio DESC`)
- `idx_tareas_fecha_cierre` (`fecha_cierre DESC`)
- `idx_tareas_usuario_id` (`usuario_id`)

---

## 6. `smartmaint.tarea_archivos`

Tabla de colección para archivos asociados a tareas.

- `tarea_id` BIGINT NOT NULL
- `archivo` VARCHAR(255)

### Relaciones
- `tarea_id` → `smartmaint.tareas(id)`

### Notas
- Esta tabla no tiene entidad JPA propia, se genera desde `@ElementCollection` en `Tarea`.

---

## 7. `smartmaint.nota_tarea`

- `id` BIGSERIAL PRIMARY KEY
- `tarea_id` BIGINT NOT NULL
- `autor_id` BIGINT
- `autor_nombre` VARCHAR(150)
- `texto` TEXT NOT NULL
- `fecha_creacion` TIMESTAMP NOT NULL

### Relaciones
- `tarea_id` → `smartmaint.tareas(id)`
- `autor_id` → `smartmaint.usuarios(id)`

---

## 8. `smartmaint.notificaciones`

- `id` BIGSERIAL PRIMARY KEY
- `usuario_id` BIGINT NOT NULL
- `mensaje` VARCHAR(255) NOT NULL
- `fecha` TIMESTAMP NOT NULL
- `leido` BOOLEAN NOT NULL DEFAULT FALSE

### Relaciones
- `usuario_id` → `smartmaint.usuarios(id)`

### Índices sugeridos
- `idx_notificaciones_usuario_leido_fecha` (`usuario_id`, `leido`, `fecha DESC`)

---

## 9. `smartmaint.refresh_tokens`

- `id` BIGSERIAL PRIMARY KEY
- `token` VARCHAR(512) NOT NULL UNIQUE
- `correo` VARCHAR(150) NOT NULL
- `expires_at` TIMESTAMP NOT NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `revoked` BOOLEAN NOT NULL DEFAULT FALSE

### Índices
- `idx_rt_token` (`token`)
- `idx_rt_correo` (`correo`)
- `idx_rt_expires` (`expires_at`)

---

## 10. `smartmaint.tarea_audit_log`

- `id` BIGSERIAL PRIMARY KEY
- `tarea_id` BIGINT NOT NULL
- `actor_correo` VARCHAR(150)
- `accion` VARCHAR(40) NOT NULL
- `estado_anterior` VARCHAR(30)
- `estado_nuevo` VARCHAR(30)
- `detalle` VARCHAR(500)
- `creado_en` TIMESTAMP NOT NULL DEFAULT NOW()

### Índices
- `idx_tarea_audit_tarea_id` (`tarea_id`)
- `idx_tarea_audit_creado_en` (`creado_en DESC`)

---

## Orden de creación sugerido para migración Flyway

1. `CREATE SCHEMA IF NOT EXISTS smartmaint;`
2. `roles`
3. `empresas`
4. `usuarios`
5. `equipos`
6. `tareas`
7. `tarea_archivos`
8. `nota_tarea`
9. `notificaciones`
10. `refresh_tokens`
11. `tarea_audit_log`
12. índices adicionales para tablas existentes

---

## SQL completo sugerido

```sql
CREATE SCHEMA IF NOT EXISTS smartmaint;

CREATE TABLE smartmaint.roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE smartmaint.empresas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    correo VARCHAR(100) NOT NULL UNIQUE,
    sector VARCHAR(50) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    activa BOOLEAN NOT NULL DEFAULT FALSE,
    token VARCHAR(255) UNIQUE
);

CREATE TABLE smartmaint.usuarios (
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
    demo BOOLEAN DEFAULT FALSE,
    expira_en TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES smartmaint.roles(id),
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES smartmaint.empresas(id)
);

CREATE TABLE smartmaint.equipos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(30),
    ubicacion VARCHAR(50),
    categoria VARCHAR(30),
    descripcion VARCHAR(120),
    created_at TIMESTAMP,
    empresa_id BIGINT NOT NULL,
    CONSTRAINT fk_equipo_empresa FOREIGN KEY (empresa_id) REFERENCES smartmaint.empresas(id),
    CONSTRAINT uk_equipo_empresa_nombre UNIQUE (empresa_id, nombre)
);

CREATE TABLE smartmaint.tareas (
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
    CONSTRAINT fk_tarea_usuario FOREIGN KEY (usuario_id) REFERENCES smartmaint.usuarios(id)
);

CREATE TABLE smartmaint.tarea_archivos (
    tarea_id BIGINT NOT NULL,
    archivo VARCHAR(255),
    CONSTRAINT fk_tarea_archivo_tarea FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id)
);

CREATE TABLE smartmaint.nota_tarea (
    id BIGSERIAL PRIMARY KEY,
    tarea_id BIGINT NOT NULL,
    autor_id BIGINT,
    autor_nombre VARCHAR(150),
    texto TEXT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_nota_tarea_tarea FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id),
    CONSTRAINT fk_nota_tarea_autor FOREIGN KEY (autor_id) REFERENCES smartmaint.usuarios(id)
);

CREATE TABLE smartmaint.notificaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    mensaje VARCHAR(255) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES smartmaint.usuarios(id)
);

CREATE TABLE smartmaint.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    correo VARCHAR(150) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE smartmaint.tarea_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tarea_id BIGINT NOT NULL,
    actor_correo VARCHAR(150),
    accion VARCHAR(40) NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30),
    detalle VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuarios_empresa_rol ON smartmaint.usuarios (empresa_id, rol_id);
CREATE INDEX idx_usuarios_demo_expira_en ON smartmaint.usuarios (demo, expira_en);
CREATE INDEX idx_tareas_estado ON smartmaint.tareas (estado);
CREATE INDEX idx_tareas_correo_colaborador ON smartmaint.tareas (correo_colaborador);
CREATE INDEX idx_tareas_fecha_inicio ON smartmaint.tareas (fecha_inicio DESC);
CREATE INDEX idx_tareas_fecha_cierre ON smartmaint.tareas (fecha_cierre DESC);
CREATE INDEX idx_tareas_usuario_id ON smartmaint.tareas (usuario_id);
CREATE INDEX idx_notificaciones_usuario_leido_fecha ON smartmaint.notificaciones (usuario_id, leido, fecha DESC);
CREATE INDEX idx_nota_tarea_tarea_fecha ON smartmaint.nota_tarea (tarea_id, fecha_creacion ASC);
CREATE INDEX idx_rt_token ON smartmaint.refresh_tokens (token);
CREATE INDEX idx_rt_correo ON smartmaint.refresh_tokens (correo);
CREATE INDEX idx_rt_expires ON smartmaint.refresh_tokens (expires_at);
CREATE INDEX idx_tarea_audit_tarea_id ON smartmaint.tarea_audit_log (tarea_id);
CREATE INDEX idx_tarea_audit_creado_en ON smartmaint.tarea_audit_log (creado_en DESC);
```

---

## Nota final

El esquema `smartmaint` es el esquema usado por el backend y Flyway está configurado para él en `application.properties`.

Si quieres, puedo generar también el archivo de migración Flyway SQL completo `V3__create_smartmaint_tables.sql` con estas definiciones.

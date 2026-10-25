
BEGIN;

CREATE TABLE IF NOT EXISTS orden_examen (
    id                SERIAL PRIMARY KEY,
    mascota_id        INTEGER      NOT NULL REFERENCES mascota(id),
    cita_id           INTEGER      REFERENCES cita(id),
    veterinario_id    INTEGER      NOT NULL REFERENCES usuario(id),
    tipo_examen       VARCHAR(150) NOT NULL,
    cirugia_prevista  VARCHAR(150),
    indicaciones      TEXT,
    estado            VARCHAR(30)  NOT NULL DEFAULT 'Pendiente',
    fecha_solicitud   TIMESTAMP    NOT NULL DEFAULT now(),
    evaluado_por      INTEGER      REFERENCES usuario(id),
    fecha_evaluacion  TIMESTAMP,
    observaciones     TEXT,
    CONSTRAINT orden_examen_estado_check
        CHECK (estado IN ('Pendiente', 'Resultados_Subidos', 'Apto_Cirugia', 'Rechazado'))
);
CREATE INDEX IF NOT EXISTS ix_orden_examen_mascota ON orden_examen(mascota_id);
CREATE INDEX IF NOT EXISTS ix_orden_examen_estado  ON orden_examen(estado);

CREATE TABLE IF NOT EXISTS archivo_examen (
    id                 SERIAL PRIMARY KEY,
    orden_id           INTEGER      NOT NULL REFERENCES orden_examen(id),
    nombre_original    VARCHAR(255) NOT NULL,
    nombre_almacenado  VARCHAR(100) NOT NULL UNIQUE,
    tipo_contenido     VARCHAR(50)  NOT NULL,
    tamano_bytes       INTEGER      NOT NULL CHECK (tamano_bytes > 0),
    subido_por         INTEGER      NOT NULL REFERENCES usuario(id),
    fecha_subida       TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_archivo_examen_orden ON archivo_examen(orden_id);

CREATE TABLE IF NOT EXISTS cirugia_programada (
    id              SERIAL PRIMARY KEY,
    orden_id        INTEGER      NOT NULL REFERENCES orden_examen(id),
    cita_id         INTEGER      REFERENCES cita(id),
    mascota_id      INTEGER      NOT NULL REFERENCES mascota(id),
    veterinario_id  INTEGER      NOT NULL REFERENCES usuario(id),
    tipo_cirugia    VARCHAR(150) NOT NULL,
    fecha_hora      TIMESTAMP    NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'Programada',
    creado_por      INTEGER      NOT NULL REFERENCES usuario(id),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT cirugia_programada_estado_check CHECK (estado IN ('Programada', 'Realizada', 'Cancelada'))
);
CREATE INDEX IF NOT EXISTS ix_cirugia_mascota ON cirugia_programada(mascota_id);

ALTER TABLE mascota ADD COLUMN IF NOT EXISTS codigo_unico_temporal VARCHAR(30);
ALTER TABLE mascota ALTER COLUMN usuario_id DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_mascota_codigo_temporal
    ON mascota(codigo_unico_temporal) WHERE codigo_unico_temporal IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'mascota_dueno_o_codigo_check') THEN
        ALTER TABLE mascota ADD CONSTRAINT mascota_dueno_o_codigo_check
            CHECK (usuario_id IS NOT NULL OR codigo_unico_temporal IS NOT NULL) NOT VALID;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS contacto_emergencia (
    id                  SERIAL PRIMARY KEY,
    mascota_id          INTEGER      NOT NULL UNIQUE REFERENCES mascota(id),
    codigo_emitido      VARCHAR(30)  NOT NULL UNIQUE,
    nombre_acompanante  VARCHAR(120) NOT NULL,
    telefono            VARCHAR(20)  NOT NULL,
    motivo              VARCHAR(300),
    fecha_ingreso       TIMESTAMP    NOT NULL DEFAULT now(),
    registrado_por      INTEGER      NOT NULL REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS lote_producto (
    id                SERIAL PRIMARY KEY,
    producto_id       INTEGER      NOT NULL REFERENCES producto(id),
    numero_lote       VARCHAR(60)  NOT NULL,
    fecha_vencimiento DATE,
    stock_actual      INTEGER      NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    fecha_ingreso     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT ux_lote_producto UNIQUE (producto_id, numero_lote)
);
CREATE INDEX IF NOT EXISTS ix_lote_producto_venc ON lote_producto(producto_id, fecha_vencimiento);

ALTER TABLE compra_detalle  ADD COLUMN IF NOT EXISTS numero_lote       VARCHAR(60);
ALTER TABLE compra_detalle  ADD COLUMN IF NOT EXISTS fecha_vencimiento DATE;
ALTER TABLE movimiento_stock ADD COLUMN IF NOT EXISTS lote_id INTEGER REFERENCES lote_producto(id);

INSERT INTO lote_producto (producto_id, numero_lote, fecha_vencimiento, stock_actual)
SELECT p.id, 'LOTE-INICIAL', p.fecha_vencimiento, p.stock
FROM producto p
WHERE p.stock > 0
  AND NOT EXISTS (SELECT 1 FROM lote_producto l WHERE l.producto_id = p.id);

COMMIT;

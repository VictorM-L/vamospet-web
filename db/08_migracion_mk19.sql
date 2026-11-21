BEGIN;

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'Cita';
ALTER TABLE cita ADD COLUMN IF NOT EXISTS enlace_videollamada VARCHAR(500);

CREATE TABLE IF NOT EXISTS cuarto (
    id      SERIAL PRIMARY KEY,
    numero  INTEGER      NOT NULL UNIQUE,
    nombre  VARCHAR(60)  NOT NULL,
    activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO cuarto (numero, nombre)
SELECT n, 'Cuarto ' || n FROM generate_series(1, 10) AS n
WHERE NOT EXISTS (SELECT 1 FROM cuarto c WHERE c.numero = n);

CREATE TABLE IF NOT EXISTS estancia (
    id               SERIAL PRIMARY KEY,
    cuarto_id        INTEGER       NOT NULL REFERENCES cuarto(id),
    mascota_id       INTEGER       NOT NULL REFERENCES mascota(id),
    usuario_id       INTEGER       REFERENCES usuario(id),
    tipo             VARCHAR(20)   NOT NULL,
    ingreso          TIMESTAMP     NOT NULL,
    salida_estimada  TIMESTAMP     NOT NULL,
    salida_real      TIMESTAMP,
    estado           VARCHAR(20)   NOT NULL DEFAULT 'Reservada',
    precio_unitario  NUMERIC(10,2) NOT NULL,
    total            NUMERIC(10,2) NOT NULL DEFAULT 0,
    cirugia_id       INTEGER       REFERENCES cirugia_programada(id),
    observaciones    TEXT,
    creado_por       INTEGER       REFERENCES usuario(id),
    fecha_creacion   TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT estancia_tipo_check CHECK (tipo IN ('Hospedaje', 'Internamiento')),
    CONSTRAINT estancia_estado_check CHECK (estado IN ('Reservada', 'En_curso', 'Finalizada', 'Cancelada')),
    CONSTRAINT estancia_fechas_check CHECK (salida_estimada > ingreso)
);
CREATE INDEX IF NOT EXISTS ix_estancia_cuarto ON estancia(cuarto_id, ingreso);
CREATE INDEX IF NOT EXISTS ix_estancia_mascota ON estancia(mascota_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_estancia_cirugia ON estancia(cirugia_id) WHERE cirugia_id IS NOT NULL AND estado <> 'Cancelada';

CREATE TABLE IF NOT EXISTS viaje_solicitud (
    id                    SERIAL PRIMARY KEY,
    usuario_id            INTEGER       NOT NULL REFERENCES usuario(id),
    fecha_viaje           DATE          NOT NULL,
    destino               VARCHAR(150),
    documento_nombre      VARCHAR(255)  NOT NULL,
    documento_almacenado  VARCHAR(100)  NOT NULL UNIQUE,
    documento_tipo        VARCHAR(50)   NOT NULL,
    cantidad              INTEGER       NOT NULL CHECK (cantidad > 0),
    total                 NUMERIC(10,2) NOT NULL,
    estado                VARCHAR(30)   NOT NULL DEFAULT 'Registrada',
    observaciones         TEXT,
    revisado_por          INTEGER       REFERENCES usuario(id),
    fecha_solicitud       TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT viaje_estado_check CHECK (estado IN ('Pago_por_confirmar', 'Registrada', 'Aprobada', 'Rechazada', 'Cancelada'))
);

CREATE TABLE IF NOT EXISTS viaje_mascota (
    id          SERIAL PRIMARY KEY,
    viaje_id    INTEGER NOT NULL REFERENCES viaje_solicitud(id),
    mascota_id  INTEGER NOT NULL REFERENCES mascota(id),
    CONSTRAINT ux_viaje_mascota UNIQUE (viaje_id, mascota_id)
);

ALTER TABLE pago ADD COLUMN IF NOT EXISTS estado            VARCHAR(20) NOT NULL DEFAULT 'Pagado';
ALTER TABLE pago ADD COLUMN IF NOT EXISTS correo_tarjeta    VARCHAR(150);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS tarjeta_ultimos4  VARCHAR(4);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS operacion_yape    VARCHAR(40);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS estancia_id       INTEGER REFERENCES estancia(id);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS viaje_id          INTEGER REFERENCES viaje_solicitud(id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pago_estado_check') THEN
        ALTER TABLE pago ADD CONSTRAINT pago_estado_check CHECK (estado IN ('Pagado', 'Por_confirmar', 'Rechazado'));
    END IF;
END $$;

INSERT INTO servicio (nombre, descripcion, precio_base, requiere_pago_previo, activo, tipo)
SELECT v.nombre, v.descripcion, v.precio, FALSE, TRUE, v.tipo
FROM (VALUES
    ('Consulta Virtual', 'Orientación veterinaria por videollamada', 60.00, 'Cita'),
    ('Hospedaje', 'Precio por noche', 60.00, 'Hospedaje'),
    ('Internamiento', 'Precio por día de internamiento', 120.00, 'Internamiento'),
    ('Viaja con tu mascota', 'Trámite de viaje: precio de la primera mascota', 150.00, 'Viaje'),
    ('Viaja con tu mascota - mascota adicional', 'Precio por cada mascota adicional del mismo viaje', 80.00, 'Viaje')
) AS v(nombre, descripcion, precio, tipo)
WHERE NOT EXISTS (SELECT 1 FROM servicio s WHERE lower(s.nombre) = lower(v.nombre));

UPDATE servicio SET tipo = 'Hospedaje' WHERE lower(nombre) = 'hospedaje';
UPDATE servicio SET activo = FALSE WHERE lower(nombre) = 'hospedaje por día';
UPDATE servicio SET tipo = 'Internamiento' WHERE lower(nombre) = 'internamiento';
UPDATE servicio SET tipo = 'Viaje' WHERE lower(nombre) LIKE 'viaja con tu mascota%';

COMMIT;

BEGIN;

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS duracion_min INTEGER NOT NULL DEFAULT 30;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS cupo_diario INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'servicio_duracion_check') THEN
        ALTER TABLE servicio ADD CONSTRAINT servicio_duracion_check CHECK (duracion_min BETWEEN 15 AND 480 AND duracion_min % 15 = 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'servicio_cupo_check') THEN
        ALTER TABLE servicio ADD CONSTRAINT servicio_cupo_check CHECK (cupo_diario IS NULL OR cupo_diario BETWEEN 1 AND 100);
    END IF;
END $$;

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS hace_grooming BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE mascota ADD COLUMN IF NOT EXISTS alergias VARCHAR(500);
ALTER TABLE mascota ADD COLUMN IF NOT EXISTS peso_kg NUMERIC(5,2);

ALTER TABLE cita ADD COLUMN IF NOT EXISTS inicio_real TIMESTAMP;
ALTER TABLE cita ADD COLUMN IF NOT EXISTS fin_real TIMESTAMP;

ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS servicio_id INTEGER REFERENCES servicio(id);
ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS inicio_real TIMESTAMP;
ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS fin_real TIMESTAMP;
ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS resultado TEXT;
ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS complicaciones TEXT;
ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS notas_intra TEXT;
ALTER TABLE cirugia_programada ADD COLUMN IF NOT EXISTS postoperatorio VARCHAR(20);

ALTER TABLE cirugia_programada DROP CONSTRAINT IF EXISTS cirugia_programada_estado_check;
ALTER TABLE cirugia_programada ADD CONSTRAINT cirugia_programada_estado_check
    CHECK (estado IN ('Por_confirmar', 'Programada', 'En_curso', 'Postoperatorio', 'Realizada', 'Cancelada'));
ALTER TABLE cirugia_programada DROP CONSTRAINT IF EXISTS cirugia_programada_postoperatorio_check;
ALTER TABLE cirugia_programada ADD CONSTRAINT cirugia_programada_postoperatorio_check
    CHECK (postoperatorio IS NULL OR postoperatorio IN ('Internar', 'Alta directa'));

UPDATE cirugia_programada c SET estado = 'Por_confirmar'
FROM cita ci
WHERE c.cita_id = ci.id AND c.estado = 'Programada' AND ci.estado = 'Pendiente';

UPDATE cirugia_programada c SET servicio_id = ci.servicio_id
FROM cita ci
WHERE c.cita_id = ci.id AND c.servicio_id IS NULL AND ci.servicio_id IS NOT NULL;

ALTER TABLE estancia ADD COLUMN IF NOT EXISTS cuidados_especiales VARCHAR(500);
ALTER TABLE estancia ALTER COLUMN cuarto_id DROP NOT NULL;
ALTER TABLE estancia DROP CONSTRAINT IF EXISTS estancia_tipo_check;
ALTER TABLE estancia ADD CONSTRAINT estancia_tipo_check CHECK (tipo IN ('Hospedaje', 'Internamiento', 'Guarderia'));
ALTER TABLE estancia DROP CONSTRAINT IF EXISTS estancia_cuarto_check;
ALTER TABLE estancia ADD CONSTRAINT estancia_cuarto_check CHECK (tipo = 'Guarderia' OR cuarto_id IS NOT NULL);

ALTER TABLE viaje_solicitud ALTER COLUMN documento_nombre DROP NOT NULL;
ALTER TABLE viaje_solicitud ALTER COLUMN documento_almacenado DROP NOT NULL;
ALTER TABLE viaje_solicitud ALTER COLUMN documento_tipo DROP NOT NULL;

CREATE TABLE IF NOT EXISTS viaje_documento (
    id                 SERIAL PRIMARY KEY,
    viaje_id           INTEGER      NOT NULL REFERENCES viaje_solicitud(id),
    requisito          VARCHAR(40)  NOT NULL,
    nombre_original    VARCHAR(255) NOT NULL,
    nombre_almacenado  VARCHAR(100) NOT NULL UNIQUE,
    tipo_contenido     VARCHAR(50)  NOT NULL,
    tamano_bytes       INTEGER      NOT NULL CHECK (tamano_bytes > 0),
    vigente            BOOLEAN      NOT NULL DEFAULT TRUE,
    estado             VARCHAR(20)  NOT NULL DEFAULT 'Pendiente' CHECK (estado IN ('Pendiente', 'Aceptado', 'Observado')),
    subido_por         INTEGER      REFERENCES usuario(id),
    subido_en          TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_viaje_documento_viaje ON viaje_documento(viaje_id, requisito);

INSERT INTO servicio (nombre, descripcion, precio_base, requiere_pago_previo, activo, tipo, duracion_min)
SELECT v.nombre, v.descripcion, v.precio, FALSE, v.activo, 'Grooming', v.duracion
FROM (VALUES
    ('Grooming - Baño',     'Baño, secado y cepillado',                     50.00, TRUE,  60),
    ('Grooming - Corte',    'Corte de pelo según raza o estilo',            40.00, TRUE,  60),
    ('Grooming - Estética', 'Corte de uñas, limpieza de oídos y perfumado',  0.00, FALSE, 90),
    ('Grooming - Completo', 'Baño, corte y estética',                       70.00, TRUE, 120)
) AS v(nombre, descripcion, precio, activo, duracion)
WHERE NOT EXISTS (SELECT 1 FROM servicio s WHERE lower(s.nombre) = lower(v.nombre));

INSERT INTO servicio (nombre, descripcion, precio_base, requiere_pago_previo, activo, tipo, duracion_min, cupo_diario)
SELECT 'Guardería por día', 'Cuidado durante el día, de 8:00 a 20:00', 0.00, FALSE, FALSE, 'Guarderia', 30, 8
WHERE NOT EXISTS (SELECT 1 FROM servicio s WHERE lower(s.nombre) = lower('Guardería por día'));

COMMIT;

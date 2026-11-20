CREATE TABLE IF NOT EXISTS banner (
    id            SERIAL PRIMARY KEY,
    seccion       VARCHAR(20)  NOT NULL CHECK (seccion IN ('Farmacia', 'Accesorios')),
    imagen_pc     VARCHAR(255) NOT NULL,
    imagen_movil  VARCHAR(255),
    orden         INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_banner_seccion_orden ON banner (seccion, activo, orden);

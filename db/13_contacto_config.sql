CREATE TABLE IF NOT EXISTS mensaje_contacto (
    id             SERIAL PRIMARY KEY,
    nombre         VARCHAR(100)  NOT NULL,
    correo         VARCHAR(150)  NOT NULL,
    telefono       VARCHAR(20),
    asunto         VARCHAR(100)  NOT NULL,
    mensaje        VARCHAR(2000) NOT NULL,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'Nuevo' CHECK (estado IN ('Nuevo', 'Respondido', 'Archivado')),
    respuesta      VARCHAR(2000),
    respondido_por INTEGER       REFERENCES usuario(id),
    ip_origen      VARCHAR(45),
    creado_en      TIMESTAMP     NOT NULL DEFAULT now(),
    respondido_en  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_mensaje_contacto_estado ON mensaje_contacto (estado, creado_en);
CREATE INDEX IF NOT EXISTS ix_mensaje_contacto_correo ON mensaje_contacto (correo, creado_en);
CREATE INDEX IF NOT EXISTS ix_mensaje_contacto_ip ON mensaje_contacto (ip_origen, creado_en);

CREATE TABLE IF NOT EXISTS configuracion_clinica (
    id                  INTEGER      PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    nombre              VARCHAR(100) NOT NULL,
    direccion           VARCHAR(200) NOT NULL,
    telefono            VARCHAR(30)  NOT NULL,
    whatsapp            VARCHAR(15)  NOT NULL,
    correo              VARCHAR(150) NOT NULL,
    horario_atencion    VARCHAR(150) NOT NULL,
    emergencias_24h     BOOLEAN      NOT NULL DEFAULT TRUE,
    hospedaje_apertura  INTEGER      NOT NULL DEFAULT 8  CHECK (hospedaje_apertura BETWEEN 0 AND 23),
    hospedaje_cierre    INTEGER      NOT NULL DEFAULT 20 CHECK (hospedaje_cierre BETWEEN 1 AND 24),
    horas_cancelacion   INTEGER      NOT NULL DEFAULT 24 CHECK (horas_cancelacion BETWEEN 1 AND 168),
    actualizado_por     INTEGER      REFERENCES usuario(id),
    actualizado_en      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT configuracion_hospedaje_check CHECK (hospedaje_cierre > hospedaje_apertura)
);

INSERT INTO configuracion_clinica (id, nombre, direccion, telefono, whatsapp, correo, horario_atencion)
VALUES (1, 'Vamos Pet Clínica Veterinaria', 'Calle San Martín 849, Miraflores, Lima', '+51 912 345 678', '51912345678',
        'contacto@vamospet.pe', 'Lun a Dom: 8:00 am – 9:00 pm')
ON CONFLICT (id) DO NOTHING;

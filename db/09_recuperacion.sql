BEGIN;

ALTER TABLE codigo_verificacion ADD COLUMN IF NOT EXISTS proposito VARCHAR(20) NOT NULL DEFAULT 'Registro';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'codigo_verificacion_proposito_check') THEN
        ALTER TABLE codigo_verificacion
            ADD CONSTRAINT codigo_verificacion_proposito_check CHECK (proposito IN ('Registro', 'Recuperacion'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_codigo_verificacion_usuario_creado
    ON codigo_verificacion (usuario_id, proposito, creado);

COMMIT;

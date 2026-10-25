
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_escritura') THEN
        CREATE ROLE app_escritura LOGIN PASSWORD 'CAMBIAR_CLAVE_ESCRITURA';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_lectura') THEN
        CREATE ROLE app_lectura LOGIN PASSWORD 'CAMBIAR_CLAVE_LECTURA';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE vamospet_db TO app_escritura, app_lectura;
GRANT USAGE ON SCHEMA public TO app_escritura, app_lectura;

GRANT SELECT                  ON ALL TABLES IN SCHEMA public TO app_lectura;
GRANT SELECT, INSERT, UPDATE  ON ALL TABLES IN SCHEMA public TO app_escritura;
GRANT USAGE, SELECT           ON ALL SEQUENCES IN SCHEMA public TO app_escritura;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT                 ON TABLES TO app_lectura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO app_escritura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT          ON SEQUENCES TO app_escritura;

REVOKE DELETE, TRUNCATE ON ALL TABLES IN SCHEMA public FROM app_escritura, app_lectura;
REVOKE CREATE ON SCHEMA public FROM app_escritura, app_lectura;


GRANT DELETE ON cita_costo_adicional, horario_atencion, veterinario_especialidad, notificacion TO app_escritura;


BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_escritura') THEN
        CREATE ROLE app_escritura LOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_lectura') THEN
        CREATE ROLE app_lectura LOGIN;
    END IF;
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_escritura, app_lectura', current_database());
END
$$;

GRANT USAGE ON SCHEMA public TO app_escritura, app_lectura;
REVOKE CREATE ON SCHEMA public FROM app_escritura, app_lectura;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM app_escritura, app_lectura;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_lectura;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO app_escritura;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_escritura;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO app_lectura;

GRANT DELETE ON banner, cita_costo_adicional, horario_atencion, veterinario_especialidad, notificacion TO app_escritura;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO app_lectura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO app_escritura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_escritura;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO app_lectura;

COMMIT;

SELECT grantee, string_agg(DISTINCT privilege_type, ', ' ORDER BY privilege_type) AS permisos, count(DISTINCT table_name) AS tablas
FROM information_schema.role_table_grants
WHERE grantee IN ('app_escritura', 'app_lectura') AND table_schema = 'public'
GROUP BY grantee
ORDER BY grantee;

SELECT table_name AS tablas_con_delete
FROM information_schema.role_table_grants
WHERE grantee = 'app_escritura' AND privilege_type = 'DELETE' AND table_schema = 'public'
ORDER BY table_name;

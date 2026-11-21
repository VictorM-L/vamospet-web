BEGIN;

DO $$
DECLARE
    c record;
    cambiadas integer;
BEGIN
    FOR c IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public' AND data_type IN ('character varying', 'text')
    LOOP
        EXECUTE format(
            'UPDATE %I SET %I = regexp_replace(%I, ''^https?://(localhost|127\.0\.0\.1):8080/uploads/'', ''/uploads/'') WHERE %I ~ ''^https?://(localhost|127\.0\.0\.1):8080/uploads/''',
            c.table_name, c.column_name, c.column_name, c.column_name);
        GET DIAGNOSTICS cambiadas = ROW_COUNT;
        IF cambiadas > 0 THEN
            RAISE NOTICE '%.%: % filas', c.table_name, c.column_name, cambiadas;
        END IF;
    END LOOP;
END
$$;

COMMIT;

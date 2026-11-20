BEGIN;

ALTER TABLE usuario ALTER COLUMN contrasena TYPE character varying(255);

COMMIT;

SELECT count(*) FILTER (WHERE contrasena IS NOT NULL AND contrasena <> '' AND contrasena NOT LIKE '$2%') AS claves_sin_cifrar,
       count(*) FILTER (WHERE contrasena LIKE '$2%') AS claves_cifradas
FROM usuario;

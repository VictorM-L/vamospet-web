BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE usuario
SET contrasena = crypt(contrasena, gen_salt('bf', 10))
WHERE contrasena IS NOT NULL AND contrasena <> '' AND contrasena NOT LIKE '$2%';

COMMIT;

SELECT count(*) FILTER (WHERE contrasena IS NOT NULL AND contrasena <> '' AND contrasena NOT LIKE '$2%') AS claves_sin_cifrar,
       count(*) FILTER (WHERE contrasena LIKE '$2%') AS claves_cifradas
FROM usuario;

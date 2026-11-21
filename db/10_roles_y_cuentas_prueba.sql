BEGIN;

ALTER TABLE cita ADD COLUMN IF NOT EXISTS motivo_estado VARCHAR(300);
ALTER TABLE cita ADD COLUMN IF NOT EXISTS cancelacion_solicitada TIMESTAMP;
ALTER TABLE cita ADD COLUMN IF NOT EXISTS motivo_solicitud VARCHAR(300);

DROP INDEX IF EXISTS ux_cita_vet_franja;
CREATE UNIQUE INDEX ux_cita_vet_franja ON cita (veterinario_id, fecha_hora)
    WHERE veterinario_id IS NOT NULL AND estado NOT IN ('Cancelada', 'Rechazada', 'Por reasignar');

CREATE INDEX IF NOT EXISTS ix_cita_estado ON cita (estado);

INSERT INTO usuario (nombre, correo, contrasena, telefono, rol_id, dni, activo)
SELECT v.nombre, v.correo, v.contrasena, v.telefono, v.rol_id, v.dni, TRUE
FROM (VALUES
    ('Prueba Administrador', 'admin.prueba@vamospet.test', 'Prueba2026', '900000101', 1, '90000101'),
    ('Prueba Veterinario',   'vet.prueba@vamospet.test',   'Prueba2026', '900000103', 3, '90000103'),
    ('Prueba Auxiliar',      'aux.prueba@vamospet.test',   'Prueba2026', '900000104', 4, '90000104'),
    ('Prueba Cliente',       'cliente.prueba@vamospet.test','Prueba2026', '900000102', 2, '90000102')
) AS v(nombre, correo, contrasena, telefono, rol_id, dni)
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE lower(u.correo) = lower(v.correo) OR u.dni = v.dni);

INSERT INTO veterinario_especialidad (veterinario_id, especialidad_id)
SELECT u.id, e.id
FROM usuario u
JOIN especialidad e ON e.nombre = 'Medicina general'
WHERE u.correo = 'vet.prueba@vamospet.test'
  AND NOT EXISTS (SELECT 1 FROM veterinario_especialidad ve WHERE ve.veterinario_id = u.id AND ve.especialidad_id = e.id);

INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin)
SELECT u.id, d.dia, TIME '09:00', TIME '13:00'
FROM usuario u
CROSS JOIN generate_series(1, 6) AS d(dia)
WHERE u.correo = 'vet.prueba@vamospet.test'
  AND NOT EXISTS (SELECT 1 FROM horario_atencion h WHERE h.veterinario_id = u.id AND h.dia_semana = d.dia);

COMMIT;

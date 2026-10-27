BEGIN;

INSERT INTO especialidad (nombre, descripcion, activo)
SELECT v.nombre, v.descripcion, TRUE
FROM (VALUES
  ('Cardiología', 'Corazón y sistema circulatorio'),
  ('Oftalmología', 'Ojos y visión'),
  ('Traumatología y ortopedia', 'Huesos, articulaciones y fracturas'),
  ('Odontología veterinaria', 'Salud dental y bucal'),
  ('Medicina interna', 'Enfermedades de órganos internos'),
  ('Diagnóstico por imagen', 'Radiografía y ecografía'),
  ('Nutrición clínica', 'Dietas y control de peso'),
  ('Etología', 'Comportamiento y conducta'),
  ('Oncología', 'Diagnóstico y tratamiento de tumores'),
  ('Medicina preventiva', 'Vacunación y desparasitación')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM especialidad e WHERE lower(e.nombre) = lower(v.nombre));

INSERT INTO servicio (nombre, descripcion, precio_base, requiere_pago_previo, activo)
SELECT v.nombre, v.descripcion, v.precio, v.previo, TRUE
FROM (VALUES
  ('Consulta de control', 'Seguimiento después de un tratamiento', 40.00, FALSE),
  ('Consulta de emergencia', 'Atención inmediata fuera de horario de cita', 120.00, FALSE),
  ('Consulta dermatológica', 'Evaluación de piel y pelaje', 70.00, FALSE),
  ('Vacuna antirrábica', 'Dosis anual contra la rabia', 35.00, FALSE),
  ('Desparasitación', 'Tratamiento interno y externo', 30.00, FALSE),
  ('Limpieza dental', 'Profilaxis dental con ultrasonido', 120.00, FALSE),
  ('Radiografía', 'Placa radiográfica digital', 90.00, FALSE),
  ('Ecografía', 'Ecografía abdominal', 110.00, FALSE),
  ('Hemograma completo', 'Análisis de sangre', 70.00, FALSE),
  ('Electrocardiograma', 'Evaluación cardíaca', 100.00, FALSE),
  ('Corte de uñas', 'Corte y limado de uñas', 15.00, FALSE),
  ('Baño medicado', 'Baño con tratamiento dermatológico', 60.00, FALSE),
  ('Colocación de microchip', 'Identificación permanente', 80.00, FALSE),
  ('Certificado de salud', 'Documento para viajes y trámites', 40.00, FALSE),
  ('Hospedaje por día', 'Hospedaje con supervisión veterinaria', 45.00, FALSE),
  ('Cirugía mayor', 'Procedimiento quirúrgico con internamiento', 450.00, TRUE),
  ('Esterilización (cirugía)', 'Ovariohisterectomía en hembras', 300.00, TRUE),
  ('Castración (cirugía)', 'Orquiectomía en machos', 280.00, TRUE)
) AS v(nombre, descripcion, precio, previo)
WHERE NOT EXISTS (SELECT 1 FROM servicio s WHERE lower(s.nombre) = lower(v.nombre));

INSERT INTO usuario (nombre, correo, contrasena, telefono, direccion, rol_id, dni, activo)
SELECT v.nombre, v.correo, 'Demo1234', v.telefono, v.direccion, v.rol, v.dni, TRUE
FROM (VALUES
  ('Dr. Mateo Demo', 'vet2@vamospet.test', '900000003', 'Av. Ficticia 102', 3, '10000003'),
  ('Dra. Camila Rojas Paredes', 'camila.rojas@vamospet.test', '900010001', 'Av. Ficticia 201', 3, '20100002'),
  ('Dr. Renzo Salazar Quispe', 'renzo.salazar@vamospet.test', '900010002', 'Av. Ficticia 202', 3, '20100003'),
  ('Dra. Valeria Mendoza Torres', 'valeria.mendoza@vamospet.test', '900010003', 'Av. Ficticia 203', 3, '20100004'),
  ('Dr. Diego Fernández Luna', 'diego.fernandez@vamospet.test', '900010004', 'Av. Ficticia 204', 3, '20100005'),
  ('Rosa Quispe Huamán', 'rosa.quispe@vamospet.test', '900010005', 'Av. Ficticia 205', 4, '20200001'),
  ('Luis Ccori Mamani', 'luis.ccori@vamospet.test', '900010006', 'Av. Ficticia 206', 4, '20200002'),
  ('Patricia Vargas Ríos', 'patricia.vargas@vamospet.test', '900100001', 'Jr. Imaginario 11', 2, '20300001'),
  ('Jorge Medina Castillo', 'jorge.medina@vamospet.test', '900100002', 'Jr. Imaginario 12', 2, '20300002'),
  ('Lucero Huamán Soto', 'lucero.huaman@vamospet.test', '900100003', 'Jr. Imaginario 13', 2, '20300003'),
  ('Marco Vilca Ramos', 'marco.vilca@vamospet.test', '900100004', 'Jr. Imaginario 14', 2, '20300004'),
  ('Sofía Paredes León', 'sofia.paredes@vamospet.test', '900100005', 'Jr. Imaginario 15', 2, '20300005'),
  ('Andrés Cárdenas Pinto', 'andres.cardenas@vamospet.test', '900100006', 'Jr. Imaginario 16', 2, '20300006'),
  ('Karen Delgado Cruz', 'karen.delgado@vamospet.test', '900100007', 'Jr. Imaginario 17', 2, '20300007'),
  ('Walter Chávez Ortiz', 'walter.chavez@vamospet.test', '900100008', 'Jr. Imaginario 18', 2, '20300008'),
  ('Fiorella Rivas Núñez', 'fiorella.rivas@vamospet.test', '900100009', 'Jr. Imaginario 19', 2, '20300009'),
  ('Esteban Lozano Vera', 'esteban.lozano@vamospet.test', '900100010', 'Jr. Imaginario 20', 2, '20300010'),
  ('Daniela Torres Aguilar', 'daniela.torres@vamospet.test', '900100011', 'Jr. Imaginario 21', 2, '20300011'),
  ('Ricardo Palomino Díaz', 'ricardo.palomino@vamospet.test', '900100012', 'Jr. Imaginario 22', 2, '20300012'),
  ('Gabriela Sánchez Mori', 'gabriela.sanchez@vamospet.test', '900100013', 'Jr. Imaginario 23', 2, '20300013'),
  ('Héctor Bravo Salas', 'hector.bravo@vamospet.test', '900100014', 'Jr. Imaginario 24', 2, '20300014'),
  ('Milagros Ayala Perea', 'milagros.ayala@vamospet.test', '900100015', 'Jr. Imaginario 25', 2, '20300015')
) AS v(nombre, correo, telefono, direccion, rol, dni)
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.dni = v.dni OR u.correo = v.correo);

INSERT INTO usuario (nombre, correo, contrasena, telefono, rol_id, dni, activo, correo_contacto)
SELECT v.nombre, NULL, NULL, v.telefono, 2, v.dni, TRUE, v.contacto
FROM (VALUES
  ('Teresa Alvarado Gil', '900100016', '20300016', 'teresa.alvarado@vamospet.test'),
  ('Óscar Benites Luna', '900100017', '20300017', NULL),
  ('Rocío Camacho Yupanqui', '900100018', '20300018', 'rocio.camacho@vamospet.test')
) AS v(nombre, telefono, dni, contacto)
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.dni = v.dni);

INSERT INTO usuario (nombre, correo, contrasena, telefono, direccion, rol_id, dni, activo)
SELECT 'Dr. Andrés Villanueva Soto', 'andres.villanueva@vamospet.test', 'Demo1234', '900010007', 'Av. Ficticia 207', 3, '20100001', TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.dni = '20100001' OR u.correo = 'andres.villanueva@vamospet.test');

INSERT INTO veterinario_especialidad (veterinario_id, especialidad_id)
SELECT u.id, e.id
FROM (VALUES
  ('4', 'Medicina preventiva'),
  ('10000002', 'Medicina preventiva'),
  ('10000003', 'Medicina general'),
  ('10000003', 'Cardiología'),
  ('20100001', 'Medicina general'),
  ('20100001', 'Oncología'),
  ('20100002', 'cirugia'),
  ('20100002', 'Traumatología y ortopedia'),
  ('20100003', 'Dermatología'),
  ('20100003', 'Medicina general'),
  ('20100004', 'Odontología veterinaria'),
  ('20100004', 'Oftalmología'),
  ('20100005', 'Medicina interna'),
  ('20100005', 'Diagnóstico por imagen'),
  ('20100005', 'Nutrición clínica')
) AS v(dni, esp)
JOIN usuario u ON u.dni = CASE v.dni WHEN '4' THEN '79461303' ELSE v.dni END
JOIN especialidad e ON lower(e.nombre) = lower(v.esp)
WHERE NOT EXISTS (SELECT 1 FROM veterinario_especialidad x WHERE x.veterinario_id = u.id AND x.especialidad_id = e.id);

INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin)
SELECT u.id, h.dia, h.ini::time, h.fin::time
FROM (VALUES
  ('79461303', 2, '09:00', '13:00'), ('79461303', 3, '09:00', '13:00'), ('79461303', 4, '09:00', '13:00'),
  ('79461303', 5, '09:00', '13:00'), ('79461303', 6, '09:00', '13:00'),
  ('10000002', 6, '09:00', '13:00'),
  ('10000003', 1, '14:00', '18:00'), ('10000003', 2, '14:00', '18:00'), ('10000003', 3, '14:00', '18:00'),
  ('10000003', 4, '14:00', '18:00'), ('10000003', 5, '14:00', '18:00'), ('10000003', 6, '09:00', '13:00'),
  ('20100001', 1, '09:00', '13:00'), ('20100001', 2, '09:00', '13:00'), ('20100001', 3, '09:00', '13:00'),
  ('20100001', 4, '09:00', '13:00'), ('20100001', 5, '09:00', '13:00'), ('20100001', 7, '09:00', '13:00'),
  ('20100002', 1, '09:00', '13:00'), ('20100002', 2, '09:00', '13:00'), ('20100002', 3, '09:00', '13:00'),
  ('20100002', 4, '09:00', '13:00'), ('20100002', 5, '09:00', '13:00'), ('20100002', 6, '09:00', '13:00'),
  ('20100002', 1, '14:00', '18:00'), ('20100002', 3, '14:00', '18:00'), ('20100002', 5, '14:00', '18:00'),
  ('20100003', 2, '14:00', '18:00'), ('20100003', 3, '14:00', '18:00'), ('20100003', 4, '14:00', '18:00'),
  ('20100003', 5, '14:00', '18:00'), ('20100003', 6, '14:00', '18:00'), ('20100003', 7, '09:00', '13:00'),
  ('20100004', 1, '09:00', '13:00'), ('20100004', 2, '09:00', '13:00'), ('20100004', 3, '09:00', '13:00'),
  ('20100004', 4, '09:00', '13:00'), ('20100004', 5, '09:00', '13:00'), ('20100004', 7, '14:00', '18:00'),
  ('20100005', 2, '15:00', '19:00'), ('20100005', 4, '15:00', '19:00'), ('20100005', 6, '15:00', '19:00'),
  ('20100005', 1, '15:00', '19:00'), ('20100005', 7, '09:00', '13:00')
) AS h(dni, dia, ini, fin)
JOIN usuario u ON u.dni = h.dni
WHERE NOT EXISTS (SELECT 1 FROM horario_atencion x
                  WHERE x.veterinario_id = u.id AND x.dia_semana = h.dia AND x.hora_inicio = h.ini::time);

INSERT INTO mascota (usuario_id, nombre, especie, raza, sexo, fecha_nacimiento)
SELECT u.id, m.nombre, m.especie, m.raza, m.sexo, m.nacimiento::date
FROM (VALUES
  ('20300001', 'Max', 'Perro', 'Labrador', 'Macho', '2019-04-12'),
  ('20300001', 'Luna', 'Gato', 'Siamés', 'Hembra', '2021-09-03'),
  ('20300002', 'Toby', 'Perro', 'Beagle', 'Macho', '2020-01-25'),
  ('20300003', 'Nala', 'Perro', 'Golden Retriever', 'Hembra', '2018-06-30'),
  ('20300003', 'Simba', 'Gato', 'Mestizo', 'Macho', '2022-02-14'),
  ('20300004', 'Coco', 'Perro', 'Poodle', 'Macho', '2017-11-08'),
  ('20300005', 'Bella', 'Perro', 'Bulldog Francés', 'Hembra', '2021-03-19'),
  ('20300005', 'Pelusa', 'Gato', 'Persa', 'Hembra', '2020-08-07'),
  ('20300006', 'Thor', 'Perro', 'Pastor Alemán', 'Macho', '2019-12-02'),
  ('20300007', 'Lola', 'Perro', 'Shih Tzu', 'Hembra', '2022-05-21'),
  ('20300007', 'Zeus', 'Perro', 'Husky Siberiano', 'Macho', '2020-10-10'),
  ('20300008', 'Mia', 'Gato', 'Angora', 'Hembra', '2023-01-16'),
  ('20300009', 'Bruno', 'Perro', 'Schnauzer', 'Macho', '2016-07-29'),
  ('20300009', 'Kira', 'Gato', 'Bengalí', 'Hembra', '2021-12-11'),
  ('20300010', 'Chispa', 'Perro', 'Chihuahua', 'Hembra', '2018-09-05'),
  ('20300011', 'Canela', 'Perro', 'Cocker Spaniel', 'Hembra', '2020-04-18'),
  ('20300011', 'Rufo', 'Perro', 'Mestizo', 'Macho', '2015-02-23'),
  ('20300012', 'Sasha', 'Gato', 'Maine Coon', 'Hembra', '2022-07-04'),
  ('20300013', 'Odie', 'Perro', 'Pug', 'Macho', '2021-06-27'),
  ('20300013', 'Mishi', 'Gato', 'Mestizo', 'Hembra', '2019-10-15'),
  ('20300014', 'Tango', 'Perro', 'Border Collie', 'Macho', '2020-03-09'),
  ('20300015', 'Pantera', 'Gato', 'British Shorthair', 'Hembra', '2021-01-30'),
  ('20300015', 'Frida', 'Perro', 'Dálmata', 'Hembra', '2019-08-22'),
  ('20300016', 'Duke', 'Perro', 'Rottweiler', 'Macho', '2018-05-13'),
  ('20300017', 'Gordo', 'Gato', 'Mestizo', 'Macho', '2017-03-06'),
  ('20300018', 'Maya', 'Perro', 'Golden Retriever', 'Hembra', '2023-04-02'),
  ('20300018', 'Oso', 'Perro', 'Chow Chow', 'Macho', '2020-11-26'),
  ('20300002', 'Nina', 'Gato', 'Ragdoll', 'Hembra', '2022-09-19')
) AS m(dni, nombre, especie, raza, sexo, nacimiento)
JOIN usuario u ON u.dni = m.dni
WHERE NOT EXISTS (SELECT 1 FROM mascota x WHERE x.usuario_id = u.id AND lower(x.nombre) = lower(m.nombre));

CREATE TEMP TABLE tmp_cita (dueno text, mascota text, vet text, servicio text, esp text, dow int, sem int, hora text, estado text) ON COMMIT DROP;
INSERT INTO tmp_cita VALUES
  ('20300001', 'Max', '20100001', 'Consulta General', 'Medicina general', 1, -2, '09:00', 'Atendida'),
  ('20300001', 'Luna', '20100001', 'Vacuna antirrábica', 'Medicina preventiva', 1, -2, '09:30', 'Atendida'),
  ('20300002', 'Toby', '20100001', 'Consulta General', 'Medicina general', 2, -2, '10:00', 'Atendida'),
  ('20300003', 'Nala', '20100004', 'Limpieza dental', 'Odontología veterinaria', 2, -2, '10:30', 'Atendida'),
  ('20300004', 'Coco', '20100003', 'Consulta dermatológica', 'Dermatología', 3, -2, '14:00', 'Atendida'),
  ('20300005', 'Bella', '20100003', 'Baño medicado', 'Dermatología', 3, -2, '15:00', 'Atendida'),
  ('20300006', 'Thor', '20100002', 'Radiografía', 'Traumatología y ortopedia', 4, -2, '09:00', 'Atendida'),
  ('20300007', 'Lola', '10000003', 'Vacunación Múltiple', 'Medicina preventiva', 4, -2, '14:30', 'Atendida'),
  ('20300007', 'Zeus', '10000003', 'Desparasitación', 'Medicina preventiva', 4, -2, '15:00', 'Atendida'),
  ('20300008', 'Mia', '20100001', 'Consulta General', 'Medicina general', 5, -2, '11:00', 'No asistió'),
  ('20300009', 'Bruno', '20100005', 'Ecografía', 'Diagnóstico por imagen', 6, -2, '15:30', 'Atendida'),
  ('20300010', 'Chispa', '10000003', 'Electrocardiograma', 'Cardiología', 6, -2, '10:00', 'Atendida'),
  ('20300011', 'Canela', '20100004', 'Consulta General', 'Oftalmología', 5, -1, '09:00', 'Atendida'),
  ('20300011', 'Rufo', '20100001', 'Consulta de control', 'Medicina general', 1, -1, '10:00', 'Atendida'),
  ('20300012', 'Sasha', '20100003', 'Consulta dermatológica', 'Dermatología', 2, -1, '15:00', 'Atendida'),
  ('20300013', 'Odie', '20100002', 'Consulta General', 'Traumatología y ortopedia', 2, -1, '09:00', 'Cancelada'),
  ('20300013', 'Mishi', '10000003', 'Vacuna antirrábica', 'Medicina preventiva', 3, -1, '14:00', 'Atendida'),
  ('20300014', 'Tango', '20100005', 'Hemograma completo', 'Medicina interna', 4, -1, '15:00', 'Atendida'),
  ('20300015', 'Pantera', '20100001', 'Consulta General', 'Medicina general', 3, -1, '11:30', 'Atendida'),
  ('20300015', 'Frida', '20100002', 'Consulta de control', 'Traumatología y ortopedia', 5, -1, '10:00', 'Atendida'),
  ('20300016', 'Duke', '20100001', 'Consulta General', 'Oncología', 5, -1, '12:00', 'Atendida'),
  ('20300017', 'Gordo', '20100005', 'Consulta General', 'Nutrición clínica', 6, -1, '16:00', 'No asistió'),
  ('20300018', 'Maya', '10000003', 'Vacunación Múltiple', 'Medicina preventiva', 1, -1, '14:30', 'Atendida'),
  ('20300018', 'Oso', '20100003', 'Corte de uñas', 'Dermatología', 6, -1, '14:30', 'Atendida'),
  ('20300001', 'Max', '20100004', 'Consulta General', 'Oftalmología', 3, 0, '10:00', 'Confirmada'),
  ('20300002', 'Nina', '20100001', 'Consulta General', 'Medicina general', 4, 0, '09:30', 'Confirmada'),
  ('20300003', 'Simba', '10000003', 'Vacuna antirrábica', 'Medicina preventiva', 4, 0, '16:00', 'Pendiente'),
  ('20300004', 'Coco', '20100003', 'Consulta de control', 'Dermatología', 5, 0, '15:30', 'Confirmada'),
  ('20300005', 'Pelusa', '20100005', 'Consulta General', 'Medicina interna', 6, 0, '16:30', 'Confirmada'),
  ('20300006', 'Thor', '20100002', 'Consulta de control', 'Traumatología y ortopedia', 6, 0, '09:30', 'Pendiente'),
  ('20300007', 'Lola', '20100004', 'Limpieza dental', 'Odontología veterinaria', 7, 0, '14:00', 'Confirmada'),
  ('20300008', 'Mia', '20100001', 'Consulta General', 'Medicina general', 7, 0, '09:30', 'Pendiente'),
  ('20300009', 'Kira', '20100003', 'Consulta dermatológica', 'Dermatología', 7, 0, '10:00', 'Confirmada'),
  ('20300012', 'Sasha', '20100001', 'Consulta General', 'Medicina general', 1, 1, '09:00', 'Confirmada'),
  ('20300013', 'Odie', '20100002', 'Consulta General', 'Traumatología y ortopedia', 2, 1, '09:30', 'Pendiente'),
  ('20300014', 'Tango', '20100005', 'Consulta de control', 'Medicina interna', 2, 1, '15:00', 'Confirmada'),
  ('20300015', 'Frida', '20100004', 'Consulta General', 'Oftalmología', 3, 1, '09:00', 'Pendiente'),
  ('20300016', 'Duke', '20100001', 'Consulta de control', 'Oncología', 4, 1, '10:00', 'Confirmada'),
  ('20300018', 'Maya', '10000003', 'Consulta General', 'Cardiología', 5, 1, '14:00', 'Pendiente');

DO $$
DECLARE
  r record;
  v_masc int; v_vet int; v_serv int; v_esp int; v_precio numeric; v_fecha timestamp; v_cita int; v_pago int;
  v_aux int; v_n int := 0; v_saltadas int := 0; v_met text[] := ARRAY['Efectivo','Yape','Tarjeta','Plin'];
  v_i int := 0;
BEGIN
  SELECT id INTO v_aux FROM usuario WHERE dni = '20200001';
  FOR r IN SELECT * FROM tmp_cita LOOP
    SELECT m.id INTO v_masc FROM mascota m JOIN usuario u ON u.id = m.usuario_id WHERE u.dni = r.dueno AND lower(m.nombre) = lower(r.mascota);
    SELECT id INTO v_vet FROM usuario WHERE dni = r.vet;
    SELECT id, precio_base INTO v_serv, v_precio FROM servicio WHERE lower(nombre) = lower(r.servicio) LIMIT 1;
    SELECT id INTO v_esp FROM especialidad WHERE lower(nombre) = lower(r.esp) LIMIT 1;
    v_fecha := current_date + ((r.dow - extract(isodow FROM current_date)::int + 7) % 7) * interval '1 day'
               + r.sem * interval '7 days' + r.hora::time;
    IF r.sem >= 0 AND v_fecha < now() THEN v_fecha := v_fecha + interval '7 days'; END IF;
    IF v_masc IS NULL OR v_vet IS NULL OR v_serv IS NULL OR v_esp IS NULL THEN v_saltadas := v_saltadas + 1; CONTINUE; END IF;
    IF NOT EXISTS (SELECT 1 FROM horario_atencion h WHERE h.veterinario_id = v_vet AND h.dia_semana = r.dow
                   AND r.hora::time >= h.hora_inicio AND r.hora::time < h.hora_fin) THEN v_saltadas := v_saltadas + 1; CONTINUE; END IF;
    IF EXISTS (SELECT 1 FROM cita c WHERE c.mascota_id = v_masc AND c.fecha_hora = v_fecha) THEN CONTINUE; END IF;
    IF EXISTS (SELECT 1 FROM cita c WHERE c.veterinario_id = v_vet AND c.fecha_hora = v_fecha AND c.estado <> 'Cancelada') THEN v_saltadas := v_saltadas + 1; CONTINUE; END IF;

    INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
    VALUES (v_masc, v_serv, v_esp, v_vet, v_fecha, r.estado, v_precio) RETURNING id INTO v_cita;
    v_n := v_n + 1;

    IF r.estado = 'Atendida' THEN
      INSERT INTO historial_evento (mascota_id, tipo_evento, fecha, descripcion, veterinario_id)
      VALUES (v_masc, 'Consulta', v_fecha,
              'Servicio: ' || r.servicio || E'\nDiagnóstico: Evaluación sin hallazgos de gravedad.' || E'\nTratamiento / indicaciones: Control en 15 días si persisten los síntomas.', v_vet);
      IF r.servicio ILIKE '%vacun%' THEN
        INSERT INTO vacuna (mascota_id, nombre, fecha_aplicacion, proxima_dosis)
        VALUES (v_masc, CASE WHEN r.servicio ILIKE '%rábica%' THEN 'Antirrábica' ELSE 'Múltiple' END, v_fecha::date, (v_fecha + interval '1 year')::date);
        INSERT INTO historial_evento (mascota_id, tipo_evento, fecha, descripcion, veterinario_id)
        VALUES (v_masc, 'Vacunación', v_fecha, 'Vacuna aplicada: ' || CASE WHEN r.servicio ILIKE '%rábica%' THEN 'Antirrábica' ELSE 'Múltiple' END, v_vet);
      END IF;
      v_i := v_i + 1;
      INSERT INTO pago (usuario_id, cita_id, monto, fecha, metodo, id_registrado_por)
      SELECT m.usuario_id, v_cita, v_precio, v_fecha + interval '45 minutes', v_met[1 + v_i % 4], v_aux
      FROM mascota m WHERE m.id = v_masc RETURNING id INTO v_pago;
      INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
      VALUES (v_pago, 'Boleta', 'B001-C' || v_cita, gen_random_uuid()::text);
    END IF;
  END LOOP;
  RAISE NOTICE 'Citas insertadas: %, saltadas: %', v_n, v_saltadas;
END $$;

INSERT INTO receta (mascota_id, veterinario_id, fecha, contenido, estado)
SELECT m.id, u.id, now() - interval '10 days', v.contenido, 'Aprobada'
FROM (VALUES
  ('20300003', 'Nala', '20100004', 'Gotas oftálmicas, 1 gota cada 8 horas por 7 días'),
  ('20300004', 'Coco', '20100003', 'Shampoo medicado cada 3 días por 3 semanas'),
  ('20300011', 'Canela', '20100004', 'Antiinflamatorio ocular cada 12 horas por 5 días'),
  ('20300016', 'Duke', '20100001', 'Suplemento articular una vez al día por 30 días')
) AS v(dni, mascota, vet, contenido)
JOIN usuario u ON u.dni = v.vet
JOIN usuario d ON d.dni = v.dni
JOIN mascota m ON m.usuario_id = d.id AND lower(m.nombre) = lower(v.mascota)
WHERE NOT EXISTS (SELECT 1 FROM receta r WHERE r.mascota_id = m.id AND r.contenido = v.contenido);

COMMIT;
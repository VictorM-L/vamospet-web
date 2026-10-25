BEGIN;

TRUNCATE usuario, categoria_producto RESTART IDENTITY CASCADE;
INSERT INTO rol (id, nombre) VALUES
(1,'Administrador'), (2,'Cliente'), (3,'Veterinario'), (4,'Auxiliar de Veterinaria')
ON CONFLICT (id) DO NOTHING;
INSERT INTO usuario (id, nombre, dni, correo, contrasena, telefono, direccion, fecha_registro, rol_id, correo_contacto, activo) VALUES
(1,'Administrador Demo','10000001','admin@vamospet.test','Demo1234','900000001','Av. Ficticia 100',NOW(),1,NULL,TRUE),
(2,'Dra. Lucía Prueba', '10000002','vet1@vamospet.test', 'Demo1234','900000002','Av. Ficticia 101',NOW(),3,NULL,TRUE),
(3,'Dr. Mateo Demo', '10000003','vet2@vamospet.test', 'Demo1234','900000003','Av. Ficticia 102',NOW(),3,NULL,TRUE),
(4,'Auxiliar Demo', '10000004','aux@vamospet.test', 'Demo1234','900000004','Av. Ficticia 103',NOW(),4,NULL,TRUE),
(5,'Carlos Cliente Demo','10000005','cliente1@vamospet.test','Demo1234','900000005','Jr. Imaginario 1',NOW(),2,NULL,TRUE),
(6,'Ana Cliente Demo', '10000006','cliente2@vamospet.test','Demo1234','900000006','Jr. Imaginario 2',NOW(),2,NULL,TRUE),
(7,'Pedro Sin Cuenta', '10000007',NULL,NULL,'900000007',NULL,NOW(),2,'sincuenta@vamospet.test',TRUE);
INSERT INTO especialidad (id, nombre, descripcion, activo) VALUES
(1,'Medicina general','Consulta y chequeo general',TRUE),
(2,'Dermatología','Piel y pelaje',TRUE),
(3,'Cirugía','Procedimientos quirúrgicos',TRUE);
INSERT INTO veterinario_especialidad (veterinario_id, especialidad_id) VALUES (2,1),(2,2),(3,1),(3,3);
INSERT INTO servicio (id, nombre, descripcion, precio_base, requiere_pago_previo, activo) VALUES
(1,'Consulta general','Chequeo clínico completo',60.00,FALSE,TRUE),
(2,'Vacunación','Aplicación de vacunas',45.00,FALSE,TRUE),
(3,'Baño y corte','Grooming completo',50.00,TRUE,TRUE),
(4,'Cirugía menor','Procedimiento ambulatorio',250.00,TRUE,TRUE);
INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin)
SELECT 2, d, TIME '09:00', TIME '13:00' FROM generate_series(1,5) AS d;
INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin) VALUES
(3,1,'14:00','18:00'), (3,3,'14:00','18:00'), (3,5,'14:00','18:00');
INSERT INTO categoria_producto (id, nombre, tipo, activo) VALUES
(1,'Antipulgas','Farmacia',TRUE), (2,'Antibióticos','Farmacia',TRUE),
(3,'Juguetes','Accesorio',TRUE), (4,'Correas y collares','Accesorio',TRUE);
INSERT INTO producto (id, nombre, descripcion, sku, precio, precio_costo, stock, stock_minimo, requiere_receta,
tipo, marca, unidad_presentacion, proveedor, fecha_vencimiento, estado, categoria_id, activo) VALUES
(1,'Antipulgas Max 10-20 kg','Pipeta mensual','FAR-001',55.00,25.00,25,5,FALSE,'Farmacia','DemoPharma','Caja x1','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+400,'Activo',1,TRUE),
(2,'Amoxicilina 500 mg','Antibiótico (con receta)','FAR-002',38.00,18.00,4,5,TRUE,'Farmacia','DemoPharma','Frasco 30 tab.','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+300,'Activo',2,TRUE),
(3,'Desparasitante Plus','Desparasitante oral','FAR-003',30.00,14.00,0,5,FALSE,'Farmacia','DemoPharma','Tableta','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+200,'Agotado',1,TRUE),
(4,'Vitaminas Pelaje','Suplemento','FAR-004',42.00,20.00,12,5,FALSE,'Farmacia','DemoPharma','Frasco','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+15,'Activo',1,TRUE),
(5,'Antiinflamatorio Vet','Antiinflamatorio','FAR-005',48.00,22.00,9,5,TRUE,'Farmacia','DemoPharma','Caja x10','Distribuidora Andina SAC (ficticia)',CURRENT_DATE-10,'Activo',2,TRUE),
(6,'Juguete Hueso','Juguete de goma','ACC-001',20.00,8.00,40,5,FALSE,'Accesorio','PetFun',NULL,'Distribuidora Andina SAC (ficticia)',NULL,'Activo',3,TRUE),
(7,'Correa Reforzada','Correa de nylon 1.5 m','ACC-002',35.00,15.00,18,5,FALSE,'Accesorio','PetFun',NULL,'Distribuidora Andina SAC (ficticia)',NULL,'Activo',4,TRUE);
INSERT INTO movimiento_stock (producto_id, tipo, cantidad, motivo, usuario_id, fecha)
SELECT id, 'Entrada', stock, 'Saldo inicial (datos de prueba)', 1, NOW() FROM producto WHERE stock > 0;
INSERT INTO proveedor (id, razon_social, ruc, contacto, telefono, correo, direccion, activo) VALUES
(1,'Distribuidora Andina SAC (ficticia)','20000000001','Juan Demo','910000001','ventas@andina.test','Calle Falsa 123',TRUE),
(2,'Accesorios Pet Perú SAC (ficticia)','20000000002','María Demo','910000002','ventas@petperu.test','Calle Falsa 456',TRUE);
INSERT INTO compra (id, proveedor_id, usuario_id, fecha, total, observacion, estado) VALUES
(1,1,1,NOW() - INTERVAL '12 days',250.00,'Reposición de antipulgas','Registrada');
INSERT INTO compra_detalle (compra_id, producto_id, cantidad, costo_unitario) VALUES (1,1,10,25.00);
INSERT INTO mascota (id, usuario_id, nombre, especie, raza, sexo, fecha_nacimiento) VALUES
(1,5,'Firulais','Perro','Mestizo','Macho','2020-03-15'),
(2,5,'Michi','Gato','Siamés','Hembra','2021-07-01'),
(3,6,'Rocky','Perro','Labrador','Macho','2019-11-20');
INSERT INTO cita (id, mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo) VALUES
(1,1,1,1,2,(CURRENT_DATE-10)+TIME '09:00','Atendida',60.00),
(2,2,2,1,2,(CURRENT_DATE-10)+TIME '09:30','Atendida',45.00),
(3,3,1,1,3,(CURRENT_DATE-9) +TIME '14:00','No asistió',60.00),
(4,1,2,1,2,(CURRENT_DATE-5) +TIME '10:00','Cancelada',45.00),
(5,3,4,3,3,(CURRENT_DATE-3) +TIME '15:00','Atendida',250.00),
(6,1,1,1,2,(CURRENT_DATE+2) +TIME '09:00','Confirmada',60.00),
(7,2,1,1,2,(CURRENT_DATE+2) +TIME '09:30','Pendiente',60.00);
INSERT INTO pedido (id, usuario_id, fecha, estado, metodo_entrega, total, canal) VALUES
(1,5,NOW() - INTERVAL '3 days','Entregado','Recojo en tienda',95.00,'Web'),
(2,6,NOW() - INTERVAL '1 day','Pendiente','Recojo en tienda',40.00,'Web');
INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario) VALUES
(1,1,1,55.00), (1,6,2,20.00), (2,6,2,20.00);
INSERT INTO pago (id, usuario_id, cita_id, pedido_id, monto, fecha, metodo, id_registrado_por) VALUES
(1,5,1,NULL, 60.00,NOW() - INTERVAL '10 days','Efectivo',1),
(2,5,2,NULL, 45.00,NOW() - INTERVAL '10 days','Yape',1),
(3,6,5,NULL,250.00,NOW() - INTERVAL '3 days','Tarjeta',1),
(4,5,NULL,1, 95.00,NOW() - INTERVAL '3 days','Yape',1),
(5,6,NULL,2, 40.00,NOW() - INTERVAL '1 day','Efectivo',1);
INSERT INTO comprobante (id, pago_id, tipo, numero, archivo, codigo_verificacion) VALUES
(1,1,'Boleta','B001-00000001',NULL,gen_random_uuid()::text),
(2,2,'Boleta','B001-00000002',NULL,gen_random_uuid()::text),
(3,3,'Boleta','B001-00000003',NULL,gen_random_uuid()::text),
(4,4,'Boleta','B001-00000004',NULL,gen_random_uuid()::text),
(5,5,'Boleta','B001-00000005',NULL,gen_random_uuid()::text);
DO $$
DECLARE t TEXT;
BEGIN
FOREACH t IN ARRAY ARRAY['rol','usuario','especialidad','servicio','categoria_producto','producto',
'proveedor','compra','mascota','cita','pedido','pago','comprobante'] LOOP
EXECUTE format('SELECT setval(pg_get_serial_sequence(%L, ''id''), COALESCE((SELECT MAX(id) FROM %I), 1))', t, t);
END LOOP;
END $$;
COMMIT;

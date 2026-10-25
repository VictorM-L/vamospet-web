BEGIN;

INSERT INTO usuario (id, nombre, dni, correo, contrasena, telefono, direccion, fecha_registro, rol_id, correo_contacto, activo)
VALUES (8,'Lucía Sin Correo','10000008',NULL,NULL,'900000008',NULL,NOW(),2,NULL,TRUE);

INSERT INTO cita (id, mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo) VALUES
 (8,3,1,1,3,(CURRENT_DATE-1)+TIME '14:30','Atendida',60.00),
 (9,2,2,1,3,(CURRENT_DATE+4)+TIME '15:00','Confirmada',45.00);

INSERT INTO cita_costo_adicional (cita_id, concepto, monto, agregado_por, fecha) VALUES
 (5,'Anestesia local',80.00,3,NOW() - INTERVAL '3 days'),
 (8,'Medicamento aplicado en consulta',25.00,3,NOW() - INTERVAL '1 day');

INSERT INTO devolucion (id, pedido_id, usuario_id, fecha, motivo, total_reembolso)
VALUES (1,1,1,NOW() - INTERVAL '2 days','Producto con defecto de fábrica',20.00);
INSERT INTO devolucion_detalle (devolucion_id, producto_id, cantidad, precio_unitario) VALUES (1,6,1,20.00);
INSERT INTO movimiento_stock (producto_id, tipo, cantidad, motivo, usuario_id, fecha)
VALUES (6,'Entrada',1,'Devolución #1 - Pedido #1',1,NOW() - INTERVAL '2 days');
UPDATE producto SET stock = stock + 1 WHERE id = 6;

INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo, leida, fecha, fecha_programada, enviado, fecha_envio, referencia) VALUES
 (1,'Stock bajo: Amoxicilina 500 mg','El producto "Amoxicilina 500 mg" quedó con stock bajo (4 unidades, mínimo configurado: 5).','Inventario',FALSE,NOW() - INTERVAL '2 days',NULL,TRUE,NOW() - INTERVAL '2 days',NULL),
 (5,'Cita confirmada','Tu cita del servicio Consulta general fue confirmada.','Cita',TRUE,NOW() - INTERVAL '4 days',NULL,TRUE,NOW() - INTERVAL '4 days',NULL),
 (5,'Recordatorio de cita','Mañana tienes cita con Firulais a las 09:00.','Recordatorio',FALSE,NOW(),
   ((CURRENT_DATE+2)+TIME '09:00') - INTERVAL '24 hours',FALSE,NULL,'cita:6');

INSERT INTO vacuna (mascota_id, nombre, fecha_aplicacion, proxima_dosis) VALUES
 (1,'Antirrábica',CURRENT_DATE-200,CURRENT_DATE+165),
 (1,'Quíntuple',CURRENT_DATE-200,CURRENT_DATE+165),
 (2,'Triple felina',CURRENT_DATE-100,CURRENT_DATE+265),
 (3,'Antirrábica',CURRENT_DATE-400,CURRENT_DATE-35);
INSERT INTO receta (mascota_id, veterinario_id, fecha, contenido, archivo_url, estado) VALUES
 (3,3,NOW() - INTERVAL '3 days','Amoxicilina 500 mg: 1 tableta cada 12 horas por 7 días.',NULL,'Pendiente');
INSERT INTO historial_evento (mascota_id, tipo_evento, fecha, descripcion, veterinario_id) VALUES
 (1,'Consulta',NOW() - INTERVAL '10 days','Chequeo general. Peso y temperatura normales.',2),
 (3,'Cirugía',NOW() - INTERVAL '3 days','Procedimiento ambulatorio sin complicaciones.',3);

INSERT INTO articulo_blog (titulo, categoria, contenido, autor, fecha_publicacion, activo) VALUES
 ('Cómo cuidar el pelaje de tu perro','Cuidados','Cepíllalo con regularidad y báñalo solo cuando sea necesario. (Texto de ejemplo.)','Equipo Vamos Pet',NOW() - INTERVAL '20 days',TRUE),
 ('Calendario básico de vacunación','Salud','Las primeras vacunas se aplican desde las seis semanas de vida. (Texto de ejemplo.)','Equipo Vamos Pet',NOW() - INTERVAL '8 days',TRUE);

DO $$
DECLARE t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY['usuario','cita','devolucion','devolucion_detalle','cita_costo_adicional','notificacion',
                           'vacuna','receta','historial_evento','articulo_blog','movimiento_stock','pedido_detalle',
                           'compra_detalle','horario_atencion'] LOOP
    EXECUTE format('SELECT setval(pg_get_serial_sequence(%L, ''id''), COALESCE((SELECT MAX(id) FROM %I), 1))', t, t);
  END LOOP;
END $$;
COMMIT;

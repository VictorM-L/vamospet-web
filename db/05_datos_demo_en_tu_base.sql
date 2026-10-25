DO $demo$
DECLARE
  v_admin int; v_vet int; v_aux int; v_c1 int; v_c2 int; v_c3 int; v_c4 int;
  m1 int; m2 int; m3 int;
  e_gen int; e_derm int; e_cir int;
  s_cons int; s_vac int; s_cir int; s_bano int;
  c_pulgas int; c_antib int; c_vit int; c_desp int; c_antiinf int;
  c_jug int; c_corr int; c_cama int; c_aseo int; c_transp int; c_comed int;
  pr1 int; pr2 int; prov1 int; prov2 int; comp1 int;
  lunes date; t_cita timestamp;
  ci1 int; ci2 int; ci3 int; ci4 int; ci5 int; ci6 int; ci7 int; ci8 int; ci9 int;
  ped1 int; ped2 int; ped3 int; pg int; dev1 int; vt record;
  jug int; antip int; correa int; k int;
BEGIN
  IF EXISTS (SELECT 1 FROM usuario WHERE correo = 'cliente1@vamospet.test') THEN
    RAISE NOTICE 'Los datos demo ya estaban cargados: no se hizo nada.';
    RETURN;
  END IF;

  SELECT min(id) INTO v_admin FROM usuario WHERE rol_id = 1 AND COALESCE(activo, true);
  IF v_admin IS NULL THEN RAISE EXCEPTION 'No hay ningún administrador en la tabla usuario.'; END IF;

  INSERT INTO especialidad (nombre, descripcion, activo)
  SELECT x.n, x.d, true FROM (VALUES ('Medicina general','Consulta y chequeo general'),
                                      ('Dermatología','Piel y pelaje'),
                                      ('Cirugía','Procedimientos quirúrgicos')) x(n,d)
  WHERE NOT EXISTS (SELECT 1 FROM especialidad e WHERE lower(e.nombre) = lower(x.n)
                    OR (lower(x.n) = 'cirugía' AND lower(e.nombre) IN ('cirugia','cirugía')));
  SELECT min(id) INTO e_gen  FROM especialidad WHERE lower(nombre) = 'medicina general';
  SELECT min(id) INTO e_derm FROM especialidad WHERE lower(nombre) = 'dermatología';
  SELECT min(id) INTO e_cir  FROM especialidad WHERE lower(nombre) IN ('cirugía','cirugia');

  INSERT INTO servicio (nombre, descripcion, precio_base, requiere_pago_previo, activo)
  SELECT 'Cirugía menor','Procedimiento ambulatorio',250.00,true,true
  WHERE NOT EXISTS (SELECT 1 FROM servicio WHERE lower(nombre) LIKE 'cirug%');
  SELECT min(id) INTO s_cons FROM servicio WHERE lower(nombre) LIKE 'consulta%';
  SELECT min(id) INTO s_vac  FROM servicio WHERE lower(nombre) LIKE 'vacunaci%';
  SELECT min(id) INTO s_cir  FROM servicio WHERE lower(nombre) LIKE 'cirug%';
  SELECT min(id) INTO s_bano FROM servicio WHERE lower(nombre) LIKE 'ba%o%';
  IF s_cons IS NULL THEN INSERT INTO servicio (nombre,descripcion,precio_base,requiere_pago_previo,activo)
     VALUES ('Consulta General','Chequeo clínico',50.00,false,true) RETURNING id INTO s_cons; END IF;
  IF s_vac IS NULL THEN INSERT INTO servicio (nombre,descripcion,precio_base,requiere_pago_previo,activo)
     VALUES ('Vacunación Múltiple','Aplicación de vacunas',45.00,false,true) RETURNING id INTO s_vac; END IF;

  INSERT INTO usuario (nombre, correo, contrasena, telefono, direccion, rol_id, dni, activo, correo_contacto)
  VALUES ('Dra. Lucía Prueba','vet1@vamospet.test','Demo1234','900000002','Av. Ficticia 101',3,'10000002',true,NULL)
  RETURNING id INTO v_vet;
  INSERT INTO usuario (nombre, correo, contrasena, telefono, direccion, rol_id, dni, activo)
  VALUES ('Auxiliar Demo','aux@vamospet.test','Demo1234','900000004','Av. Ficticia 103',4,'10000004',true)
  RETURNING id INTO v_aux;
  INSERT INTO usuario (nombre, correo, contrasena, telefono, direccion, rol_id, dni, activo)
  VALUES ('Carlos Cliente Demo','cliente1@vamospet.test','Demo1234','900000005','Jr. Imaginario 1',2,'10000005',true)
  RETURNING id INTO v_c1;
  INSERT INTO usuario (nombre, correo, contrasena, telefono, direccion, rol_id, dni, activo)
  VALUES ('Ana Cliente Demo','cliente2@vamospet.test','Demo1234','900000006','Jr. Imaginario 2',2,'10000006',true)
  RETURNING id INTO v_c2;
  INSERT INTO usuario (nombre, correo, contrasena, telefono, rol_id, dni, activo, correo_contacto)
  VALUES ('Pedro Sin Cuenta',NULL,NULL,'900000007',2,'10000007',true,'sincuenta@vamospet.test')
  RETURNING id INTO v_c3;
  INSERT INTO usuario (nombre, correo, contrasena, telefono, rol_id, dni, activo)
  VALUES ('Lucía Sin Correo',NULL,NULL,'900000008',2,'10000008',true)
  RETURNING id INTO v_c4;

  INSERT INTO mascota (usuario_id, nombre, especie, raza, sexo, fecha_nacimiento) VALUES
    (v_c1,'Firulais','Perro','Mestizo','Macho','2020-03-15') RETURNING id INTO m1;
  INSERT INTO mascota (usuario_id, nombre, especie, raza, sexo, fecha_nacimiento) VALUES
    (v_c1,'Michi','Gato','Siamés','Hembra','2021-07-01') RETURNING id INTO m2;
  INSERT INTO mascota (usuario_id, nombre, especie, raza, sexo, fecha_nacimiento) VALUES
    (v_c2,'Rocky','Perro','Labrador','Macho','2019-11-20') RETURNING id INTO m3;

  INSERT INTO veterinario_especialidad (veterinario_id, especialidad_id) VALUES (v_vet,e_gen),(v_vet,e_derm);
  FOR vt IN SELECT id FROM usuario WHERE rol_id = 3 AND COALESCE(activo,true) ORDER BY id LOOP
    INSERT INTO veterinario_especialidad (veterinario_id, especialidad_id)
    SELECT vt.id, e_gen WHERE NOT EXISTS (SELECT 1 FROM veterinario_especialidad WHERE veterinario_id = vt.id AND especialidad_id = e_gen);
    IF NOT EXISTS (SELECT 1 FROM veterinario_especialidad WHERE veterinario_id = vt.id AND especialidad_id = e_cir) AND vt.id <> v_vet THEN
      INSERT INTO veterinario_especialidad (veterinario_id, especialidad_id) VALUES (vt.id, e_cir);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM horario_atencion WHERE veterinario_id = vt.id) THEN
      IF vt.id = v_vet THEN
        INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin)
        SELECT vt.id, d, TIME '09:00', TIME '13:00' FROM generate_series(1,5) d;
      ELSE
        INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin) VALUES
          (vt.id,1,'14:00','18:00'),(vt.id,3,'14:00','18:00'),(vt.id,5,'14:00','18:00');
      END IF;
    END IF;
  END LOOP;

  INSERT INTO categoria_producto (nombre, tipo, activo)
  SELECT x.n, x.t, true FROM (VALUES
    ('Antipulgas','Farmacia'),('Antibióticos','Farmacia'),('Vitaminas y suplementos','Farmacia'),
    ('Desparasitantes','Farmacia'),('Antiinflamatorios','Farmacia'),('Correas y collares','Accesorio'),
    ('Juguetes','Accesorio'),('Camas y descanso','Accesorio'),('Aseo e higiene','Accesorio'),
    ('Transportadoras','Accesorio'),('Comederos y bebederos','Accesorio')) x(n,t)
  WHERE NOT EXISTS (SELECT 1 FROM categoria_producto c WHERE c.nombre = x.n AND c.tipo = x.t);
  SELECT min(id) INTO c_pulgas FROM categoria_producto WHERE nombre='Antipulgas' AND tipo='Farmacia';
  SELECT min(id) INTO c_antib  FROM categoria_producto WHERE nombre='Antibióticos' AND tipo='Farmacia';
  SELECT min(id) INTO c_vit    FROM categoria_producto WHERE nombre='Vitaminas y suplementos' AND tipo='Farmacia';
  SELECT min(id) INTO c_desp   FROM categoria_producto WHERE nombre='Desparasitantes' AND tipo='Farmacia';
  SELECT min(id) INTO c_antiinf FROM categoria_producto WHERE nombre='Antiinflamatorios' AND tipo='Farmacia';
  SELECT min(id) INTO c_corr   FROM categoria_producto WHERE nombre='Correas y collares' AND tipo='Accesorio';
  SELECT min(id) INTO c_jug    FROM categoria_producto WHERE nombre='Juguetes' AND tipo='Accesorio';
  SELECT min(id) INTO c_cama   FROM categoria_producto WHERE nombre='Camas y descanso' AND tipo='Accesorio';
  SELECT min(id) INTO c_aseo   FROM categoria_producto WHERE nombre='Aseo e higiene' AND tipo='Accesorio';
  SELECT min(id) INTO c_transp FROM categoria_producto WHERE nombre='Transportadoras' AND tipo='Accesorio';
  SELECT min(id) INTO c_comed  FROM categoria_producto WHERE nombre='Comederos y bebederos' AND tipo='Accesorio';

  INSERT INTO proveedor (razon_social, ruc, contacto, telefono, correo, direccion, activo)
  VALUES ('Distribuidora Andina SAC (ficticia)','20000000001','Juan Demo','910000001','ventas@andina.test','Calle Falsa 123',true)
  ON CONFLICT (ruc) DO NOTHING;
  INSERT INTO proveedor (razon_social, ruc, contacto, telefono, correo, direccion, activo)
  VALUES ('Accesorios Pet Perú SAC (ficticia)','20000000002','María Demo','910000002','ventas@petperu.test','Calle Falsa 456',true)
  ON CONFLICT (ruc) DO NOTHING;
  SELECT id INTO prov1 FROM proveedor WHERE ruc='20000000001';

  INSERT INTO producto (nombre, descripcion, sku, precio, precio_costo, stock, stock_minimo, requiere_receta, tipo, marca,
                        unidad_presentacion, proveedor, fecha_vencimiento, estado, categoria_id, activo) VALUES
   ('Antipulgas Max 10-20 kg','Pipeta mensual','FAR-D01',55.00,25.00,25,5,false,'Farmacia','DemoPharma','Caja x1','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+400,'Activo',c_pulgas,true),
   ('Amoxicilina 500 mg','Antibiótico (con receta) — STOCK BAJO','FAR-D02',38.00,18.00,4,5,true,'Farmacia','DemoPharma','Frasco 30 tab.','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+300,'Activo',c_antib,true),
   ('Desparasitante Plus','Desparasitante oral — SIN STOCK','FAR-D03',30.00,14.00,0,5,false,'Farmacia','DemoPharma','Tableta','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+200,'Agotado',c_desp,true),
   ('Vitaminas Pelaje','Suplemento — POR VENCER','FAR-D04',42.00,20.00,12,5,false,'Farmacia','DemoPharma','Frasco','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+15,'Activo',c_vit,true),
   ('Antiinflamatorio Vet','Antiinflamatorio (con receta) — VENCIDO','FAR-D05',48.00,22.00,9,5,true,'Farmacia','DemoPharma','Caja x10','Distribuidora Andina SAC (ficticia)',CURRENT_DATE-10,'Activo',c_antiinf,true),
   ('Probiótico Digestivo','Apoyo digestivo','FAR-D06',36.00,16.00,30,5,false,'Farmacia','DemoPharma','Frasco','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+500,'Activo',c_vit,true),
   ('Pipeta Prueba (stock 3)','Úsalo para probar ventas sin stock suficiente','FAR-D07',10.00,5.00,3,1,false,'Farmacia','DemoPharma','Caja x1','Distribuidora Andina SAC (ficticia)',CURRENT_DATE+365,'Activo',c_pulgas,true),
   ('Juguete Hueso','Juguete de goma','ACC-D01',20.00,8.00,40,5,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Activo',c_jug,true),
   ('Correa Reforzada','Correa de nylon 1.5 m','ACC-D02',35.00,15.00,18,5,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Activo',c_corr,true),
   ('Cama Ortopédica M','Cama acolchada mediana','ACC-D03',89.00,45.00,6,5,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Activo',c_cama,true),
   ('Shampoo de Avena','Shampoo hipoalergénico 300 ml','ACC-D04',28.00,12.00,22,5,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Activo',c_aseo,true),
   ('Transportadora Mediana','Transportadora rígida','ACC-D05',120.00,70.00,8,3,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Activo',c_transp,true),
   ('Comedero de Acero','Comedero antideslizante','ACC-D06',25.00,10.00,15,5,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Activo',c_comed,true),
   ('Collar Antipulgas','Collar de 8 meses','ACC-D07',45.00,20.00,0,5,false,'Accesorio','PetFun',NULL,'Accesorios Pet Perú SAC (ficticia)',NULL,'Agotado',c_corr,true);
  SELECT id INTO antip FROM producto WHERE sku='FAR-D01';
  SELECT id INTO jug   FROM producto WHERE sku='ACC-D01';
  SELECT id INTO correa FROM producto WHERE sku='ACC-D02';

  INSERT INTO compra (proveedor_id, usuario_id, fecha, total, observacion, estado)
  VALUES (prov1, v_admin, now() - interval '12 days', 250.00, 'Reposición de antipulgas (demo)', 'Registrada') RETURNING id INTO comp1;
  INSERT INTO compra_detalle (compra_id, producto_id, cantidad, costo_unitario) VALUES (comp1, antip, 10, 25.00);

  INSERT INTO pedido (usuario_id, fecha, estado, metodo_entrega, total, canal)
  VALUES (v_c1, now() - interval '3 days','Entregado','Recojo en tienda',95.00,'Web') RETURNING id INTO ped1;
  INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario) VALUES (ped1,antip,1,55.00),(ped1,jug,2,20.00);
  INSERT INTO pago (usuario_id, pedido_id, monto, fecha, metodo, id_registrado_por)
  VALUES (v_c1, ped1, 95.00, now() - interval '3 days','Yape', v_c1) RETURNING id INTO pg;
  INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
  VALUES (pg,'Boleta','B001-D'||lpad(pg::text,6,'0'),replace(gen_random_uuid()::text,'-',''));

  INSERT INTO pedido (usuario_id, fecha, estado, metodo_entrega, total, canal)
  VALUES (v_c2, now() - interval '1 day','Pendiente','Recojo en tienda',40.00,'Web') RETURNING id INTO ped2;
  INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario) VALUES (ped2,jug,2,20.00);
  INSERT INTO pago (usuario_id, pedido_id, monto, fecha, metodo, id_registrado_por)
  VALUES (v_c2, ped2, 40.00, now() - interval '1 day','Tarjeta', v_c2) RETURNING id INTO pg;
  INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
  VALUES (pg,'Boleta','B001-D'||lpad(pg::text,6,'0'),replace(gen_random_uuid()::text,'-',''));

  INSERT INTO pedido (usuario_id, fecha, estado, metodo_entrega, total, canal)
  VALUES (v_c1, now() - interval '6 days','Entregado','Recojo en tienda',35.00,'Mostrador') RETURNING id INTO ped3;
  INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario) VALUES (ped3,correa,1,35.00);
  INSERT INTO pago (usuario_id, pedido_id, monto, fecha, metodo, id_registrado_por)
  VALUES (v_c1, ped3, 35.00, now() - interval '6 days','Efectivo', v_aux) RETURNING id INTO pg;
  INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
  VALUES (pg,'Boleta','B001-D'||lpad(pg::text,6,'0'),replace(gen_random_uuid()::text,'-',''));

  INSERT INTO movimiento_stock (producto_id, tipo, cantidad, motivo, usuario_id, fecha)
  SELECT p.id,'Entrada', p.stock + COALESCE((SELECT sum(d.cantidad) FROM pedido_detalle d WHERE d.producto_id = p.id AND d.pedido_id IN (ped1,ped2,ped3)),0),
         'Saldo inicial (datos de prueba)', v_admin, now() - interval '30 days'
  FROM producto p WHERE p.sku LIKE '%-D0%' AND p.stock + COALESCE((SELECT sum(d.cantidad) FROM pedido_detalle d WHERE d.producto_id = p.id AND d.pedido_id IN (ped1,ped2,ped3)),0) > 0;
  INSERT INTO movimiento_stock (producto_id, tipo, cantidad, motivo, usuario_id, fecha)
  SELECT d.producto_id,'Salida', d.cantidad, 'Venta - Pedido #'||d.pedido_id, NULL, now() - interval '2 days'
  FROM pedido_detalle d WHERE d.pedido_id IN (ped1,ped2,ped3);

  INSERT INTO devolucion (pedido_id, usuario_id, fecha, motivo, total_reembolso)
  VALUES (ped1, v_admin, now() - interval '2 days','Producto con defecto de fábrica',20.00) RETURNING id INTO dev1;
  INSERT INTO devolucion_detalle (devolucion_id, producto_id, cantidad, precio_unitario) VALUES (dev1, jug, 1, 20.00);
  INSERT INTO movimiento_stock (producto_id, tipo, cantidad, motivo, usuario_id, fecha)
  VALUES (jug,'Entrada',1,'Devolución #'||dev1||' - Pedido #'||ped1, v_admin, now() - interval '2 days');
  UPDATE producto SET stock = stock + 1 WHERE id = jug;

  lunes := (date_trunc('week', CURRENT_DATE) + interval '1 week')::date;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m1, s_cons, e_gen, v_vet, (CURRENT_DATE-10) + TIME '09:00', 'Atendida', 50.00) RETURNING id INTO ci1;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m2, s_vac, e_gen, v_vet, (CURRENT_DATE-10) + TIME '09:30', 'Atendida', 45.00) RETURNING id INTO ci2;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m3, s_cons, e_gen, v_vet, (CURRENT_DATE-9) + TIME '10:00', 'No asistió', 50.00) RETURNING id INTO ci3;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m1, s_vac, e_gen, v_vet, (CURRENT_DATE-5) + TIME '10:00', 'Cancelada', 45.00) RETURNING id INTO ci4;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m3, s_cir, e_cir, v_vet, (CURRENT_DATE-3) + TIME '11:00', 'Atendida', 250.00) RETURNING id INTO ci5;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m1, s_cons, e_gen, v_vet, (CURRENT_DATE-1) + TIME '11:30', 'Atendida', 50.00) RETURNING id INTO ci6;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m1, s_cons, e_gen, v_vet, lunes + TIME '09:00', 'Confirmada', 50.00) RETURNING id INTO ci7;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m2, s_cons, e_gen, v_vet, lunes + TIME '09:30', 'Pendiente', 50.00) RETURNING id INTO ci8;
  INSERT INTO cita (mascota_id, servicio_id, especialidad_id, veterinario_id, fecha_hora, estado, costo)
  VALUES (m3, s_cons, e_gen, v_vet, (lunes + 1) + TIME '10:00', 'Confirmada', 50.00) RETURNING id INTO ci9;

  INSERT INTO cita_costo_adicional (cita_id, concepto, monto, agregado_por, fecha) VALUES
    (ci5,'Anestesia local',80.00,v_vet, now() - interval '3 days'),
    (ci6,'Medicamento aplicado en consulta',25.00,v_vet, now() - interval '1 day');

  INSERT INTO pago (usuario_id, cita_id, monto, fecha, metodo, id_registrado_por)
  VALUES (v_c1, ci1, 50.00, now() - interval '10 days','Efectivo', v_aux) RETURNING id INTO pg;
  INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
  VALUES (pg,'Boleta','B001-D'||lpad(pg::text,6,'0'),replace(gen_random_uuid()::text,'-',''));
  INSERT INTO pago (usuario_id, cita_id, monto, fecha, metodo, id_registrado_por)
  VALUES (v_c1, ci2, 45.00, now() - interval '10 days','Yape', v_aux) RETURNING id INTO pg;
  INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
  VALUES (pg,'Boleta','B001-D'||lpad(pg::text,6,'0'),replace(gen_random_uuid()::text,'-',''));
  INSERT INTO pago (usuario_id, cita_id, monto, fecha, metodo, id_registrado_por)
  VALUES (v_c2, ci5, 330.00, now() - interval '3 days','Tarjeta', v_aux) RETURNING id INTO pg;
  INSERT INTO comprobante (pago_id, tipo, numero, codigo_verificacion)
  VALUES (pg,'Boleta','B001-D'||lpad(pg::text,6,'0'),replace(gen_random_uuid()::text,'-',''));

  INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo, leida, fecha, enviado, fecha_envio) VALUES
    (v_admin,'Stock bajo: Amoxicilina 500 mg','El producto "Amoxicilina 500 mg" quedó con stock bajo (4 unidades, mínimo configurado: 5).','Inventario',false, now() - interval '2 days', true, now() - interval '2 days'),
    (v_c1,'Cita confirmada','Tu cita de Consulta General fue confirmada.','Cita',true, now() - interval '4 days', true, now() - interval '4 days'),
    (v_c1,'Pedido entregado','Tu pedido fue entregado. ¡Gracias por tu compra!','Pedido',false, now() - interval '3 days', true, now() - interval '3 days');

  INSERT INTO vacuna (mascota_id, nombre, fecha_aplicacion, proxima_dosis) VALUES
    (m1,'Antirrábica',CURRENT_DATE-200,CURRENT_DATE+165),
    (m1,'Quíntuple',CURRENT_DATE-200,CURRENT_DATE+165),
    (m2,'Triple felina',CURRENT_DATE-100,CURRENT_DATE+265),
    (m3,'Antirrábica',CURRENT_DATE-400,CURRENT_DATE-35);
  INSERT INTO receta (mascota_id, veterinario_id, fecha, contenido, estado)
  VALUES (m3, v_vet, now() - interval '3 days','Amoxicilina 500 mg: 1 tableta cada 12 horas por 7 días.','Pendiente');
  INSERT INTO historial_evento (mascota_id, tipo_evento, fecha, descripcion, veterinario_id) VALUES
    (m1,'Consulta', now() - interval '10 days','Chequeo general. Peso y temperatura normales.', v_vet),
    (m3,'Cirugía',  now() - interval '3 days','Procedimiento ambulatorio sin complicaciones.', v_vet);
  INSERT INTO articulo_blog (titulo, categoria, contenido, autor, fecha_publicacion, activo) VALUES
    ('Cómo cuidar el pelaje de tu perro','Cuidados','Cepíllalo con regularidad y báñalo solo cuando sea necesario. (Texto de ejemplo.)','Equipo Vamos Pet', now() - interval '20 days', true),
    ('Calendario básico de vacunación','Salud','Las primeras vacunas se aplican desde las seis semanas de vida. (Texto de ejemplo.)','Equipo Vamos Pet', now() - interval '8 days', true);

  RAISE NOTICE 'Datos demo cargados correctamente.';
END
$demo$;

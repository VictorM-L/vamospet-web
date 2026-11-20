# Manual de Organización y Funciones (MOF) — Sistema Vamos Pet

Este documento se genera a partir de la matriz de permisos del código (`backend/src/main/java/com/vamospet/backend/permisos/PermisosApi.java`). El servidor aplica exactamente estas reglas: cualquier operación que no figure aquí para un rol responde 403.

## Resumen por rol

| Función | Administrador | Veterinario | Auxiliar | Cliente |
|---|---|---|---|---|
| Gestionar personal y restablecer contraseña | S | N | N | N |
| Servicios, precios, horarios, configuración | S | N | N | N |
| Agendar y comprar | N | N | N | S (propio) |
| Confirmar cita | N | S | N | N |
| Rechazar, cancelar, "No asistió" | N | N | S | S (propia, 24 h o más) |
| Atender, recetas, exámenes, cirugías, internamiento | L | S | N | L (propio) |
| Tablero de cirugías | L | S (las suyas) | L (estado y cobro) | N |
| Alergias y peso de la mascota | L | S | N | L (propio) |
| Cobros y confirmar Yape | L | N | S | N |
| Inventario, compras, devoluciones, pedidos | L | N | S | N |
| Banners de Farmacia y Accesorios | L | N | S | N |
| Responder mensajes de contacto | L | N | S | N |
| Datos de la clínica, horario de hospedaje y plazo de cancelación | S | N | N | N |
| Hospedaje y guardería (ingreso y salida) | L | N | S | N |
| Revisar documentos de viaje (uno por requisito) | L | N | S | N |
| Habilitar a un veterinario para grooming | S | N | N | N |
| Finanzas y reportes | L | N | L (por cobrar) | N |

S = puede hacerlo · L = solo lectura · N = no puede.

## Administrador

**Propósito del puesto:** Supervisar la clínica y administrar su configuración. Ve todo el sistema en modo supervisión, pero no ejecuta las operaciones del día a día.

**Funciones principales:**

- Dar de alta al personal y enviar el restablecimiento de contraseña (nunca ve ni escribe contraseñas).
- Definir servicios con su tipo, duración, cupo diario y precio; especialidades, horarios de los veterinarios y el blog.
- Indicar qué veterinarios atienden grooming.
- Mantener los datos de la clínica (dirección, teléfono, WhatsApp, correo, horario), el horario del hospedaje y las horas mínimas para cancelar una cita.
- Revisar reportes de gestión (ingresos, veterinarios, asistencia, inventario, comprobantes).
- Supervisar citas, inventario, compras, devoluciones, cobros, estancias y emergencias en solo lectura.

**No le corresponde:**

- Comprar en la tienda o agendar citas.
- Atender, emitir recetas, solicitar exámenes o programar cirugías.
- Cobrar, confirmar pagos Yape o tocar el inventario.

**Operaciones habilitadas en el sistema:**

| Área | Operación | Tipo |
|---|---|---|
| Atención clínica | Descargar resultado de examen (cliente: solo los suyos) | Lectura |
| Atención clínica | Imprimir ticket de emergencia | Lectura |
| Atención clínica | Ver cirugías (cliente: solo las suyas) | Lectura |
| Atención clínica | Ver datos de una mascota | Lectura |
| Atención clínica | Ver emergencias sin dueño | Lectura |
| Atención clínica | Ver exámenes por evaluar | Lectura |
| Atención clínica | Ver historial clínico | Lectura |
| Atención clínica | Ver línea de tiempo de la cirugía | Lectura |
| Atención clínica | Ver mascotas de un cliente | Lectura |
| Atención clínica | Ver recetas | Lectura |
| Atención clínica | Ver tablero de cirugías (veterinario: solo las suyas; auxiliar: estado y cobro) | Lectura |
| Atención clínica | Ver todas las mascotas | Lectura |
| Atención clínica | Ver vacunas | Lectura |
| Atención clínica | Ver órdenes de examen (cliente: solo las suyas) | Lectura |
| Citas | Ver citas de un cliente | Lectura |
| Citas | Ver citas de una mascota | Lectura |
| Citas | Ver costos adicionales (el veterinario no ve totales) | Lectura |
| Citas | Ver todas las citas | Lectura |
| Cobros y finanzas | Descargar comprobante en PDF (cliente: solo los suyos) | Lectura |
| Cobros y finanzas | Descargar reporte en Excel | Lectura |
| Cobros y finanzas | Descargar reporte en PDF | Lectura |
| Cobros y finanzas | Ver comprobante de un pedido | Lectura |
| Cobros y finanzas | Ver comprobantes (cliente: solo los suyos) | Lectura |
| Cobros y finanzas | Ver pagos | Lectura |
| Cobros y finanzas | Ver pagos de un cliente | Lectura |
| Cobros y finanzas | Ver pagos por cobrar o por confirmar | Lectura |
| Cobros y finanzas | Ver reportes de gestión | Lectura |
| Cobros y finanzas | Ver resumen de caja | Lectura |
| Cuenta y personal | Cambiar rol de un usuario | Escritura |
| Cuenta y personal | Cerrar sesión e invalidar el token | Escritura |
| Cuenta y personal | Crear rol | Escritura |
| Cuenta y personal | Dar de alta personal | Escritura |
| Cuenta y personal | Dar de baja (personal: solo Admin) | Escritura |
| Cuenta y personal | Enviar restablecimiento de contraseña al personal | Escritura |
| Cuenta y personal | Indicar si un veterinario atiende grooming | Escritura |
| Cuenta y personal | Reactivar (personal: solo Admin) | Escritura |
| Cuenta y personal | Renovar el token de la sesión (cliente: 20 minutos más; personal: hasta 8 horas desde el ingreso) | Escritura |
| Cuenta y personal | Buscar cliente por DNI | Lectura |
| Cuenta y personal | Ver lista de usuarios | Lectura |
| Cuenta y personal | Ver roles | Lectura |
| General | Enviar aviso manual | Escritura |
| General | Marcar mis notificaciones como leídas | Escritura |
| General | Marcar una notificación como leída (solo propias) | Escritura |
| General | Usar el asistente virtual | Escritura |
| General | Ver bandeja de mensajes de contacto | Lectura |
| General | Ver mis notificaciones | Lectura |
| Hospedaje, internamiento y viajes | Descargar documento de viaje | Lectura |
| Hospedaje, internamiento y viajes | Descargar un documento de viaje por requisito | Lectura |
| Hospedaje, internamiento y viajes | Ver cuartos libres | Lectura |
| Hospedaje, internamiento y viajes | Ver estado de los cuartos | Lectura |
| Hospedaje, internamiento y viajes | Ver estancias (cliente: solo las suyas) | Lectura |
| Hospedaje, internamiento y viajes | Ver solicitudes de viaje (cliente: solo las suyas) | Lectura |
| Servicios, horarios y catálogo | Asignar especialidades a un veterinario | Escritura |
| Servicios, horarios y catálogo | Cambiar la configuración de la clínica | Escritura |
| Servicios, horarios y catálogo | Crear o editar especialidad | Escritura |
| Servicios, horarios y catálogo | Crear o editar servicio y precio | Escritura |
| Servicios, horarios y catálogo | Dar de baja especialidad | Escritura |
| Servicios, horarios y catálogo | Dar de baja servicio | Escritura |
| Servicios, horarios y catálogo | Definir horario de veterinario | Escritura |
| Servicios, horarios y catálogo | Ocultar artículo | Escritura |
| Servicios, horarios y catálogo | Publicar artículo | Escritura |
| Servicios, horarios y catálogo | Quitar horario de veterinario | Escritura |
| Servicios, horarios y catálogo | Reactivar artículo | Escritura |
| Servicios, horarios y catálogo | Reactivar especialidad | Escritura |
| Servicios, horarios y catálogo | Reactivar servicio | Escritura |
| Servicios, horarios y catálogo | Ver especialidades de un veterinario | Lectura |
| Servicios, horarios y catálogo | Ver horarios de atención | Lectura |
| Tienda e inventario | Ver alertas de stock y vencimiento | Lectura |
| Tienda e inventario | Ver compras | Lectura |
| Tienda e inventario | Ver detalle de compra | Lectura |
| Tienda e inventario | Ver detalle de devolución | Lectura |
| Tienda e inventario | Ver devoluciones | Lectura |
| Tienda e inventario | Ver lotes de un producto | Lectura |
| Tienda e inventario | Ver lotes por vencer | Lectura |
| Tienda e inventario | Ver movimientos de stock | Lectura |
| Tienda e inventario | Ver pedidos de un cliente | Lectura |
| Tienda e inventario | Ver productos de un pedido | Lectura |
| Tienda e inventario | Ver productos más vendidos | Lectura |
| Tienda e inventario | Ver proveedores | Lectura |
| Tienda e inventario | Ver qué se puede devolver de un pedido | Lectura |
| Tienda e inventario | Ver todos los banners de una sección | Lectura |
| Tienda e inventario | Ver todos los pedidos | Lectura |
| Tienda e inventario | Ver valorización del inventario | Lectura |

## Veterinario

**Propósito del puesto:** Atender a las mascotas y llevar su información clínica.

**Funciones principales:**

- Confirmar con un clic sus citas pendientes y avisar "No puedo atender" con un motivo para que el Auxiliar la reasigne.
- Registrar la atención: diagnóstico, tratamiento, vacunas y costos adicionales usados.
- Emitir y aprobar recetas; solicitar y evaluar exámenes; programar cirugías eligiendo el servicio quirúrgico.
- Llevar cada cirugía en el tablero, paso a paso: confirmar, iniciar (solo con examen apto y vigente), registrar notas, finalizar con resultado y receta, y decidir internamiento o alta directa.
- Registrar alergias y peso de la mascota.
- Enviar el enlace de videollamada de las consultas virtuales y atender las emergencias registradas.

**No le corresponde:**

- Cancelar citas o marcar "No asistió".
- Cobrar o ver totales y pagos (solo ve el concepto y el monto de los extras que él registra).
- Gestionar inventario, compras, hospedaje o documentos de viaje.

**Operaciones habilitadas en el sistema:**

| Área | Operación | Tipo |
|---|---|---|
| Atención clínica | Agregar evento al historial | Escritura |
| Atención clínica | Aprobar o rechazar receta | Escritura |
| Atención clínica | Avanzar la cirugía al siguiente paso | Escritura |
| Atención clínica | Cambiar estado de cirugía | Escritura |
| Atención clínica | Evaluar examen (apto o rechazado) | Escritura |
| Atención clínica | Guardar notas durante la cirugía | Escritura |
| Atención clínica | Programar cirugía | Escritura |
| Atención clínica | Registrar alergias y peso de una mascota | Escritura |
| Atención clínica | Registrar atención | Escritura |
| Atención clínica | Registrar vacuna | Escritura |
| Atención clínica | Solicitar examen | Escritura |
| Atención clínica | Subir receta (cliente o auxiliar con la receta en papel) o emitir receta (veterinario) | Escritura |
| Atención clínica | Descargar resultado de examen (cliente: solo los suyos) | Lectura |
| Atención clínica | Imprimir ticket de emergencia | Lectura |
| Atención clínica | Ver cirugías (cliente: solo las suyas) | Lectura |
| Atención clínica | Ver datos de una mascota | Lectura |
| Atención clínica | Ver emergencias sin dueño | Lectura |
| Atención clínica | Ver exámenes por evaluar | Lectura |
| Atención clínica | Ver historial clínico | Lectura |
| Atención clínica | Ver línea de tiempo de la cirugía | Lectura |
| Atención clínica | Ver mascotas de un cliente | Lectura |
| Atención clínica | Ver recetas | Lectura |
| Atención clínica | Ver tablero de cirugías (veterinario: solo las suyas; auxiliar: estado y cobro) | Lectura |
| Atención clínica | Ver todas las mascotas | Lectura |
| Atención clínica | Ver vacunas | Lectura |
| Atención clínica | Ver órdenes de examen (cliente: solo las suyas) | Lectura |
| Citas | Agregar costo adicional | Escritura |
| Citas | Avisar que no puede atender (pasa a por reasignar) | Escritura |
| Citas | Confirmar cita | Escritura |
| Citas | Enviar enlace de videollamada | Escritura |
| Citas | Quitar costo adicional | Escritura |
| Citas | Ver citas de una mascota | Lectura |
| Citas | Ver costos adicionales (el veterinario no ve totales) | Lectura |
| Citas | Ver todas las citas | Lectura |
| Cuenta y personal | Cerrar sesión e invalidar el token | Escritura |
| Cuenta y personal | Renovar el token de la sesión (cliente: 20 minutos más; personal: hasta 8 horas desde el ingreso) | Escritura |
| Cuenta y personal | Ver lista de usuarios | Lectura |
| General | Marcar mis notificaciones como leídas | Escritura |
| General | Marcar una notificación como leída (solo propias) | Escritura |
| General | Usar el asistente virtual | Escritura |
| General | Ver mis notificaciones | Lectura |
| Hospedaje, internamiento y viajes | Internar mascota operada | Escritura |
| Hospedaje, internamiento y viajes | Registrar ingreso (auxiliar: hospedaje; veterinario: internamiento) | Escritura |
| Hospedaje, internamiento y viajes | Registrar salida (auxiliar: hospedaje; veterinario: alta de internamiento) | Escritura |
| Hospedaje, internamiento y viajes | Ver estado de los cuartos | Lectura |
| Hospedaje, internamiento y viajes | Ver estancias (cliente: solo las suyas) | Lectura |
| Servicios, horarios y catálogo | Ver especialidades de un veterinario | Lectura |
| Servicios, horarios y catálogo | Ver horarios de atención | Lectura |

## Auxiliar de Veterinaria

**Propósito del puesto:** Atender la recepción, la caja y la tienda de la clínica.

**Funciones principales:**

- Rechazar, cancelar y marcar "No asistió"; atender la cola "Por reasignar" y las solicitudes de cancelación tardía.
- Cobrar citas y estancias, vender en mostrador y confirmar o rechazar pagos Yape.
- En mostrador, vender productos con receta solo con una receta aprobada y vigente del cliente; si trae la receta en papel, registrarla (foto) para que un veterinario la apruebe.
- Gestionar inventario y precios de productos, proveedores, compras, devoluciones y pedidos.
- Atender la bandeja de mensajes de contacto: responder por correo (con el mensaje original citado) y archivar.
- Subir, reemplazar, ordenar, activar o desactivar y eliminar los banners de Farmacia y Accesorios (máximo 6 activos por sección).
- Registrar clientes sin cuenta e invitarlos a crear su cuenta; registrar ingresos de emergencia.
- Registrar el ingreso y la salida de hospedaje y guardería, cancelar reservas y revisar los documentos de viaje uno por uno, marcando cuál está observado.
- Ver en el tablero de cirugías el estado de cada una y si ya se cobró, sin datos clínicos.
- Subir el archivo de resultados de examen que trae el cliente (la evaluación es del veterinario).

**No le corresponde:**

- Ver o modificar información clínica: historial, recetas, diagnósticos, evaluación de exámenes o cirugías.

**Operaciones habilitadas en el sistema:**

| Área | Operación | Tipo |
|---|---|---|
| Atención clínica | Registrar ingreso de emergencia | Escritura |
| Atención clínica | Subir archivo de resultado que trae el cliente | Escritura |
| Atención clínica | Subir receta (cliente o auxiliar con la receta en papel) o emitir receta (veterinario) | Escritura |
| Atención clínica | Descargar resultado de examen (cliente: solo los suyos) | Lectura |
| Atención clínica | Imprimir ticket de emergencia | Lectura |
| Atención clínica | Ver cirugías (cliente: solo las suyas) | Lectura |
| Atención clínica | Ver datos de una mascota | Lectura |
| Atención clínica | Ver emergencias sin dueño | Lectura |
| Atención clínica | Ver mascotas de un cliente | Lectura |
| Atención clínica | Ver recetas aprobadas y vigentes para comprar | Lectura |
| Atención clínica | Ver tablero de cirugías (veterinario: solo las suyas; auxiliar: estado y cobro) | Lectura |
| Atención clínica | Ver todas las mascotas | Lectura |
| Atención clínica | Ver órdenes de examen (cliente: solo las suyas) | Lectura |
| Citas | Reasignar cita a otro veterinario | Escritura |
| Citas | Rechazar, cancelar o marcar No asistió | Escritura |
| Citas | Resolver solicitud de cancelación tardía | Escritura |
| Citas | Ver citas de un cliente | Lectura |
| Citas | Ver citas de una mascota | Lectura |
| Citas | Ver costos adicionales (el veterinario no ve totales) | Lectura |
| Citas | Ver todas las citas | Lectura |
| Citas | Ver veterinarios libres para reasignar | Lectura |
| Cobros y finanzas | Cobrar cita | Escritura |
| Cobros y finanzas | Confirmar o rechazar Yape | Escritura |
| Cobros y finanzas | Emitir comprobante | Escritura |
| Cobros y finanzas | Pagar (cliente) o cobrar en mostrador (auxiliar) | Escritura |
| Cobros y finanzas | Registrar pago manual | Escritura |
| Cobros y finanzas | Descargar comprobante en PDF (cliente: solo los suyos) | Lectura |
| Cobros y finanzas | Ver comprobante de un pedido | Lectura |
| Cobros y finanzas | Ver comprobantes (cliente: solo los suyos) | Lectura |
| Cobros y finanzas | Ver pagos | Lectura |
| Cobros y finanzas | Ver pagos de un cliente | Lectura |
| Cobros y finanzas | Ver pagos por cobrar o por confirmar | Lectura |
| Cobros y finanzas | Ver resumen de caja | Lectura |
| Cuenta y personal | Cerrar sesión e invalidar el token | Escritura |
| Cuenta y personal | Dar de baja (personal: solo Admin) | Escritura |
| Cuenta y personal | Invitar a un cliente de mostrador a crear su cuenta | Escritura |
| Cuenta y personal | Reactivar (personal: solo Admin) | Escritura |
| Cuenta y personal | Registrar cliente sin cuenta | Escritura |
| Cuenta y personal | Registrar correo de contacto de un cliente | Escritura |
| Cuenta y personal | Renovar el token de la sesión (cliente: 20 minutos más; personal: hasta 8 horas desde el ingreso) | Escritura |
| Cuenta y personal | Buscar cliente por DNI | Lectura |
| Cuenta y personal | Ver lista de usuarios | Lectura |
| General | Archivar un mensaje de contacto | Escritura |
| General | Marcar mis notificaciones como leídas | Escritura |
| General | Marcar una notificación como leída (solo propias) | Escritura |
| General | Responder un mensaje de contacto por correo | Escritura |
| General | Subir imagen (receta del cliente, foto de producto) | Escritura |
| General | Usar el asistente virtual | Escritura |
| General | Ver bandeja de mensajes de contacto | Lectura |
| General | Ver mis notificaciones | Lectura |
| Hospedaje, internamiento y viajes | Aprobar o rechazar documento de viaje | Escritura |
| Hospedaje, internamiento y viajes | Cancelar reserva de hospedaje | Escritura |
| Hospedaje, internamiento y viajes | Registrar ingreso (auxiliar: hospedaje; veterinario: internamiento) | Escritura |
| Hospedaje, internamiento y viajes | Registrar salida (auxiliar: hospedaje; veterinario: alta de internamiento) | Escritura |
| Hospedaje, internamiento y viajes | Descargar documento de viaje | Lectura |
| Hospedaje, internamiento y viajes | Descargar un documento de viaje por requisito | Lectura |
| Hospedaje, internamiento y viajes | Ver cuartos libres | Lectura |
| Hospedaje, internamiento y viajes | Ver estado de los cuartos | Lectura |
| Hospedaje, internamiento y viajes | Ver estancias (cliente: solo las suyas) | Lectura |
| Hospedaje, internamiento y viajes | Ver solicitudes de viaje (cliente: solo las suyas) | Lectura |
| Servicios, horarios y catálogo | Ver especialidades de un veterinario | Lectura |
| Servicios, horarios y catálogo | Ver horarios de atención | Lectura |
| Tienda e inventario | Activar o desactivar un banner | Escritura |
| Tienda e inventario | Agregar imagen de producto | Escritura |
| Tienda e inventario | Anular una venta de mostrador que no se cobró (devuelve stock y libera la receta) | Escritura |
| Tienda e inventario | Cambiar el orden de un banner | Escritura |
| Tienda e inventario | Cambiar estado de pedido (al cancelarlo devuelve el stock y libera la receta) | Escritura |
| Tienda e inventario | Comprar en la web (cliente) o vender en mostrador (auxiliar), con receta si el producto la pide | Escritura |
| Tienda e inventario | Crear o editar categoría | Escritura |
| Tienda e inventario | Crear producto | Escritura |
| Tienda e inventario | Crear proveedor | Escritura |
| Tienda e inventario | Dar de baja categoría | Escritura |
| Tienda e inventario | Dar de baja producto | Escritura |
| Tienda e inventario | Dar de baja proveedor | Escritura |
| Tienda e inventario | Editar producto y precio | Escritura |
| Tienda e inventario | Editar proveedor | Escritura |
| Tienda e inventario | Eliminar banner | Escritura |
| Tienda e inventario | Quitar imagen de producto | Escritura |
| Tienda e inventario | Reactivar categoría | Escritura |
| Tienda e inventario | Reactivar producto | Escritura |
| Tienda e inventario | Reactivar proveedor | Escritura |
| Tienda e inventario | Reemplazar imágenes de un banner | Escritura |
| Tienda e inventario | Registrar compra a proveedor | Escritura |
| Tienda e inventario | Registrar devolución | Escritura |
| Tienda e inventario | Registrar entrada, salida o ajuste de stock | Escritura |
| Tienda e inventario | Subir banner | Escritura |
| Tienda e inventario | Validar stock antes de vender | Escritura |
| Tienda e inventario | Ver alertas de stock y vencimiento | Lectura |
| Tienda e inventario | Ver compras | Lectura |
| Tienda e inventario | Ver detalle de compra | Lectura |
| Tienda e inventario | Ver detalle de devolución | Lectura |
| Tienda e inventario | Ver devoluciones | Lectura |
| Tienda e inventario | Ver lotes de un producto | Lectura |
| Tienda e inventario | Ver lotes por vencer | Lectura |
| Tienda e inventario | Ver movimientos de stock | Lectura |
| Tienda e inventario | Ver pedidos de un cliente | Lectura |
| Tienda e inventario | Ver productos de un pedido | Lectura |
| Tienda e inventario | Ver productos más vendidos | Lectura |
| Tienda e inventario | Ver proveedores | Lectura |
| Tienda e inventario | Ver qué se puede devolver de un pedido | Lectura |
| Tienda e inventario | Ver todos los banners de una sección | Lectura |
| Tienda e inventario | Ver todos los pedidos | Lectura |
| Tienda e inventario | Ver valorización del inventario | Lectura |

## Cliente

**Propósito del puesto:** Gestionar sus propias mascotas, citas y compras.

**Funciones principales:**

- Registrar sus mascotas y editar su perfil.
- Agendar citas y grooming (paquete, quién lo atiende y una hora con tiempo para todo el servicio); cancelarlas con 24 horas o más de anticipación, o solicitar la cancelación si falta menos.
- Comprar en la tienda, pagar y descargar sus comprobantes. Los productos con receta se compran eligiendo una receta aprobada de su mascota (vale 30 días desde la aprobación y una sola compra).
- Ver el historial, recetas, vacunas y exámenes de sus mascotas, y subir resultados o recetas.
- Reservar hospedaje por noche o guardería por día, con cuidados especiales.
- Solicitar viajes subiendo un documento por requisito y reenviar solo el que fue observado.

**No le corresponde:**

- Ver o modificar información de otros clientes.

**Operaciones habilitadas en el sistema:**

| Área | Operación | Tipo |
|---|---|---|
| Atención clínica | Registrar mi mascota | Escritura |
| Atención clínica | Subir receta (cliente o auxiliar con la receta en papel) o emitir receta (veterinario) | Escritura |
| Atención clínica | Subir resultado de examen de mi mascota | Escritura |
| Atención clínica | Vincular mascota de emergencia con código | Escritura |
| Atención clínica | Descargar resultado de examen (cliente: solo los suyos) | Lectura |
| Atención clínica | Ver cirugías (cliente: solo las suyas) | Lectura |
| Atención clínica | Ver órdenes de examen (cliente: solo las suyas) | Lectura |
| Atención clínica | Ver datos de una mascota | Lectura (solo lo propio) |
| Atención clínica | Ver historial clínico | Lectura (solo lo propio) |
| Atención clínica | Ver mascotas de un cliente | Lectura (solo lo propio) |
| Atención clínica | Ver recetas | Lectura (solo lo propio) |
| Atención clínica | Ver recetas aprobadas y vigentes para comprar | Lectura (solo lo propio) |
| Atención clínica | Ver vacunas | Lectura (solo lo propio) |
| Citas | Agendar cita | Escritura |
| Citas | Cancelar mi cita (24 h o más antes) | Escritura |
| Citas | Solicitar cancelación (menos de 24 h) | Escritura |
| Citas | Ver citas de un cliente | Lectura (solo lo propio) |
| Citas | Ver citas de una mascota | Lectura (solo lo propio) |
| Cobros y finanzas | Pagar (cliente) o cobrar en mostrador (auxiliar) | Escritura |
| Cobros y finanzas | Descargar comprobante en PDF (cliente: solo los suyos) | Lectura |
| Cobros y finanzas | Ver comprobantes (cliente: solo los suyos) | Lectura |
| Cobros y finanzas | Ver pagos por cobrar o por confirmar | Lectura |
| Cobros y finanzas | Ver comprobante de un pedido | Lectura (solo lo propio) |
| Cobros y finanzas | Ver pagos de un cliente | Lectura (solo lo propio) |
| Cuenta y personal | Cerrar sesión e invalidar el token | Escritura |
| Cuenta y personal | Renovar el token de la sesión (cliente: 20 minutos más; personal: hasta 8 horas desde el ingreso) | Escritura |
| Cuenta y personal | Editar mi perfil | Escritura (solo lo propio) |
| General | Marcar una notificación como leída (solo propias) | Escritura |
| General | Subir imagen (receta del cliente, foto de producto) | Escritura |
| General | Usar el asistente virtual | Escritura |
| General | Marcar mis notificaciones como leídas | Escritura (solo lo propio) |
| General | Ver mis notificaciones | Lectura (solo lo propio) |
| Hospedaje, internamiento y viajes | Reenviar documentos observados de mi viaje | Escritura |
| Hospedaje, internamiento y viajes | Reservar hospedaje | Escritura |
| Hospedaje, internamiento y viajes | Solicitar viaje con documento | Escritura |
| Hospedaje, internamiento y viajes | Descargar documento de viaje | Lectura |
| Hospedaje, internamiento y viajes | Descargar un documento de viaje por requisito | Lectura |
| Hospedaje, internamiento y viajes | Ver cuartos libres | Lectura |
| Hospedaje, internamiento y viajes | Ver estancias (cliente: solo las suyas) | Lectura |
| Hospedaje, internamiento y viajes | Ver solicitudes de viaje (cliente: solo las suyas) | Lectura |
| Tienda e inventario | Comprar en la web (cliente) o vender en mostrador (auxiliar), con receta si el producto la pide | Escritura |
| Tienda e inventario | Validar stock antes de vender | Escritura |
| Tienda e inventario | Ver pedidos de un cliente | Lectura (solo lo propio) |
| Tienda e inventario | Ver productos de un pedido | Lectura (solo lo propio) |

## Operaciones públicas (sin iniciar sesión)

- Iniciar sesión
- Crear cuenta de cliente
- Confirmar cuenta con código
- Consultar si un DNI o correo ya está registrado
- Pedir código de recuperación
- Verificar código de recuperación
- Crear nueva contraseña con código
- Ver especialidades con veterinario disponible
- Ver veterinarios de una especialidad
- Ver horarios libres
- Ver veterinarios que atienden grooming
- Ver catálogo de productos
- Ver banners de Farmacia o Accesorios
- Ver un producto
- Ver imágenes de un producto
- Ver categorías
- Enviar mensaje de contacto
- Verificar autenticidad de un comprobante
- Ver precios y horario de hospedaje
- Ver tarifa de viajes
- Ver documentos que pide un viaje
- Ver servicios y precios
- Ver datos de contacto y horarios de la clínica
- Ver especialidades
- Ver artículos del blog

## Reglas transversales

- El sistema identifica a la persona en cada operación; una cuenta dada de baja no puede operar.
- Un cliente solo accede a sus mascotas, citas, pedidos, pagos y comprobantes.
- Nadie, ni siquiera el Administrador, lee las notificaciones de otra persona.
- Nadie del personal escribe ni ve la contraseña de otra persona: siempre se crea con un código enviado al correo.
- Total de operaciones controladas: 183.

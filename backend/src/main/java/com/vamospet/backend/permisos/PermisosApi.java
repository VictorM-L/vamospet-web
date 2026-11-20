package com.vamospet.backend.permisos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.vamospet.backend.permisos.Propiedad.MASCOTA;
import static com.vamospet.backend.permisos.Propiedad.PEDIDO;
import static com.vamospet.backend.permisos.Propiedad.USUARIO;

public final class PermisosApi {

    public static final String CUENTA = "Cuenta y personal";
    public static final String CLINICA = "Atención clínica";
    public static final String CITAS = "Citas";
    public static final String COMERCIAL = "Tienda e inventario";
    public static final String COBROS = "Cobros y finanzas";
    public static final String ESTANCIAS = "Hospedaje, internamiento y viajes";
    public static final String CATALOGO = "Servicios, horarios y catálogo";
    public static final String GENERAL = "General";

    private static final Set<Rol> A = EnumSet.of(Rol.ADMIN);
    private static final Set<Rol> C = EnumSet.of(Rol.CLIENTE);
    private static final Set<Rol> V = EnumSet.of(Rol.VETERINARIO);
    private static final Set<Rol> X = EnumSet.of(Rol.AUXILIAR);
    private static final Set<Rol> AX = EnumSet.of(Rol.ADMIN, Rol.AUXILIAR);
    private static final Set<Rol> AV = EnumSet.of(Rol.ADMIN, Rol.VETERINARIO);
    private static final Set<Rol> CX = EnumSet.of(Rol.CLIENTE, Rol.AUXILIAR);
    private static final Set<Rol> VX = EnumSet.of(Rol.VETERINARIO, Rol.AUXILIAR);
    private static final Set<Rol> AVX = EnumSet.of(Rol.ADMIN, Rol.VETERINARIO, Rol.AUXILIAR);
    private static final Set<Rol> ACX = EnumSet.of(Rol.ADMIN, Rol.CLIENTE, Rol.AUXILIAR);
    private static final Set<Rol> ACV = EnumSet.of(Rol.ADMIN, Rol.CLIENTE, Rol.VETERINARIO);
    private static final Set<Rol> TODOS = EnumSet.allOf(Rol.class);

    private static final List<Regla> REGLAS;

    static {
        List<Regla> r = new ArrayList<>();

        r.add(Regla.publica("POST", "/api/usuarios/login", CUENTA, "Iniciar sesión"));
        r.add(Regla.de("POST", "/api/sesion/renovar", TODOS, CUENTA, "Renovar el token de la sesión (cliente: 20 minutos más; personal: hasta 8 horas desde el ingreso)"));
        r.add(Regla.de("POST", "/api/sesion/cerrar", TODOS, CUENTA, "Cerrar sesión e invalidar el token"));
        r.add(Regla.publica("POST", "/api/usuarios/registro", CUENTA, "Crear cuenta de cliente"));
        r.add(Regla.publica("POST", "/api/usuarios/registro/confirmar", CUENTA, "Confirmar cuenta con código"));
        r.add(Regla.publica("GET", "/api/usuarios/existe", CUENTA, "Consultar si un DNI o correo ya está registrado"));
        r.add(Regla.publica("POST", "/api/usuarios/recuperar/solicitar", CUENTA, "Pedir código de recuperación"));
        r.add(Regla.publica("POST", "/api/usuarios/recuperar/verificar", CUENTA, "Verificar código de recuperación"));
        r.add(Regla.publica("POST", "/api/usuarios/recuperar/restablecer", CUENTA, "Crear nueva contraseña con código"));
        r.add(Regla.de("GET", "/api/usuarios", AVX, CUENTA, "Ver lista de usuarios"));
        r.add(Regla.de("GET", "/api/usuarios/buscar-dni/{dni}", AX, CUENTA, "Buscar cliente por DNI"));
        r.add(Regla.de("POST", "/api/usuarios/personal", A, CUENTA, "Dar de alta personal"));
        r.add(Regla.de("POST", "/api/usuarios/personal/{id}/restablecimiento", A, CUENTA, "Enviar restablecimiento de contraseña al personal"));
        r.add(Regla.de("POST", "/api/usuarios/cliente-sin-cuenta", X, CUENTA, "Registrar cliente sin cuenta"));
        r.add(Regla.de("POST", "/api/usuarios/{id}/invitacion", X, CUENTA, "Invitar a un cliente de mostrador a crear su cuenta"));
        r.add(Regla.de("PATCH", "/api/usuarios/{id}/correo-contacto", X, CUENTA, "Registrar correo de contacto de un cliente"));
        r.add(Regla.propia("PUT", "/api/usuarios/{id}/perfil", C, USUARIO, "id", CUENTA, "Editar mi perfil"));
        r.add(Regla.de("PATCH", "/api/usuarios/{id}/rol", A, CUENTA, "Cambiar rol de un usuario"));
        r.add(Regla.de("PATCH", "/api/usuarios/{id}/baja", AX, CUENTA, "Dar de baja (personal: solo Admin)"));
        r.add(Regla.de("PATCH", "/api/usuarios/{id}/reactivar", AX, CUENTA, "Reactivar (personal: solo Admin)"));
        r.add(Regla.de("PATCH", "/api/usuarios/{id}/grooming", A, CUENTA, "Indicar si un veterinario atiende grooming"));
        r.add(Regla.de("GET", "/api/roles", A, CUENTA, "Ver roles"));
        r.add(Regla.de("POST", "/api/roles", A, CUENTA, "Crear rol"));

        r.add(Regla.publica("GET", "/api/agenda/especialidades", CITAS, "Ver especialidades con veterinario disponible"));
        r.add(Regla.publica("GET", "/api/agenda/veterinarios", CITAS, "Ver veterinarios de una especialidad"));
        r.add(Regla.publica("GET", "/api/agenda/disponibilidad", CITAS, "Ver horarios libres"));
        r.add(Regla.publica("GET", "/api/agenda/grooming/veterinarios", CITAS, "Ver veterinarios que atienden grooming"));
        r.add(Regla.de("GET", "/api/citas", AVX, CITAS, "Ver todas las citas"));
        r.add(Regla.propia("GET", "/api/citas/mascota/{mascotaId}", TODOS, MASCOTA, "mascotaId", CITAS, "Ver citas de una mascota"));
        r.add(Regla.propia("GET", "/api/citas/usuario/{usuarioId}", ACX, USUARIO, "usuarioId", CITAS, "Ver citas de un cliente"));
        r.add(Regla.de("POST", "/api/citas", C, CITAS, "Agendar cita"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/confirmar", V, CITAS, "Confirmar cita"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/no-puedo-atender", V, CITAS, "Avisar que no puede atender (pasa a por reasignar)"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/estado", X, CITAS, "Rechazar, cancelar o marcar No asistió"));
        r.add(Regla.de("GET", "/api/citas/{id}/veterinarios-disponibles", X, CITAS, "Ver veterinarios libres para reasignar"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/reasignar", X, CITAS, "Reasignar cita a otro veterinario"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/solicitud-cancelacion/resolver", X, CITAS, "Resolver solicitud de cancelación tardía"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/cancelar", C, CITAS, "Cancelar mi cita (24 h o más antes)"));
        r.add(Regla.de("POST", "/api/citas/{id}/solicitud-cancelacion", C, CITAS, "Solicitar cancelación (menos de 24 h)"));
        r.add(Regla.de("PATCH", "/api/citas/{id}/videollamada", V, CITAS, "Enviar enlace de videollamada"));
        r.add(Regla.de("POST", "/api/citas/{id}/atender", V, CLINICA, "Registrar atención"));
        r.add(Regla.de("POST", "/api/citas/{id}/cobrar", X, COBROS, "Cobrar cita"));
        r.add(Regla.de("GET", "/api/citas/{citaId}/costos-adicionales", AVX, CITAS, "Ver costos adicionales (el veterinario no ve totales)"));
        r.add(Regla.de("POST", "/api/citas/{citaId}/costos-adicionales", V, CITAS, "Agregar costo adicional"));
        r.add(Regla.de("DELETE", "/api/citas/{citaId}/costos-adicionales/{costoId}", V, CITAS, "Quitar costo adicional"));

        r.add(Regla.de("GET", "/api/mascotas", AVX, CLINICA, "Ver todas las mascotas"));
        r.add(Regla.propia("GET", "/api/mascotas/usuario/{usuarioId}", TODOS, USUARIO, "usuarioId", CLINICA, "Ver mascotas de un cliente"));
        r.add(Regla.propia("GET", "/api/mascotas/{id}", TODOS, MASCOTA, "id", CLINICA, "Ver datos de una mascota"));
        r.add(Regla.de("PATCH", "/api/mascotas/{id}/datos-clinicos", V, CLINICA, "Registrar alergias y peso de una mascota"));
        r.add(Regla.de("POST", "/api/mascotas", C, CLINICA, "Registrar mi mascota"));
        r.add(Regla.propia("GET", "/api/historial/mascota/{mascotaId}", ACV, MASCOTA, "mascotaId", CLINICA, "Ver historial clínico"));
        r.add(Regla.de("POST", "/api/historial", V, CLINICA, "Agregar evento al historial"));
        r.add(Regla.propia("GET", "/api/vacunas/mascota/{mascotaId}", ACV, MASCOTA, "mascotaId", CLINICA, "Ver vacunas"));
        r.add(Regla.de("POST", "/api/vacunas", V, CLINICA, "Registrar vacuna"));
        r.add(Regla.propia("GET", "/api/recetas/mascota/{mascotaId}", ACV, MASCOTA, "mascotaId", CLINICA, "Ver recetas"));
        r.add(Regla.de("POST", "/api/recetas", EnumSet.of(Rol.CLIENTE, Rol.VETERINARIO, Rol.AUXILIAR), CLINICA, "Subir receta (cliente o auxiliar con la receta en papel) o emitir receta (veterinario)"));
        r.add(Regla.propia("GET", "/api/recetas/vigentes/cliente/{clienteId}", CX, USUARIO, "clienteId", CLINICA, "Ver recetas aprobadas y vigentes para comprar"));
        r.add(Regla.de("PATCH", "/api/recetas/{id}/estado", V, CLINICA, "Aprobar o rechazar receta"));
        r.add(Regla.de("POST", "/api/veterinario/examenes/solicitar", V, CLINICA, "Solicitar examen"));
        r.add(Regla.de("GET", "/api/veterinario/examenes/pendientes", AV, CLINICA, "Ver exámenes por evaluar"));
        r.add(Regla.de("PUT", "/api/veterinario/examenes/evaluar/{idOrden}", V, CLINICA, "Evaluar examen (apto o rechazado)"));
        r.add(Regla.de("POST", "/api/veterinario/cirugias/agendar", V, CLINICA, "Programar cirugía"));
        r.add(Regla.de("PATCH", "/api/veterinario/cirugias/{id}/estado", V, CLINICA, "Cambiar estado de cirugía"));
        r.add(Regla.de("POST", "/api/auxiliar/examenes/subir", X, CLINICA, "Subir archivo de resultado que trae el cliente"));
        r.add(Regla.de("POST", "/api/cliente/examenes/subir-pdf", C, CLINICA, "Subir resultado de examen de mi mascota"));
        r.add(Regla.de("GET", "/api/examenes", TODOS, CLINICA, "Ver órdenes de examen (cliente: solo las suyas)"));
        r.add(Regla.de("GET", "/api/examenes/archivo/{id}", TODOS, CLINICA, "Descargar resultado de examen (cliente: solo los suyos)"));
        r.add(Regla.de("GET", "/api/cirugias", TODOS, CLINICA, "Ver cirugías (cliente: solo las suyas)"));
        r.add(Regla.de("GET", "/api/cirugias/tablero", AVX, CLINICA, "Ver tablero de cirugías (veterinario: solo las suyas; auxiliar: estado y cobro)"));
        r.add(Regla.de("PATCH", "/api/cirugias/{id}/siguiente", V, CLINICA, "Avanzar la cirugía al siguiente paso"));
        r.add(Regla.de("PATCH", "/api/cirugias/{id}/notas", V, CLINICA, "Guardar notas durante la cirugía"));
        r.add(Regla.de("GET", "/api/cirugias/{id}/linea-tiempo", AV, CLINICA, "Ver línea de tiempo de la cirugía"));
        r.add(Regla.de("POST", "/api/auxiliar/emergencia/registrar", X, CLINICA, "Registrar ingreso de emergencia"));
        r.add(Regla.de("GET", "/api/auxiliar/emergencia/pendientes", AVX, CLINICA, "Ver emergencias sin dueño"));
        r.add(Regla.de("GET", "/api/auxiliar/emergencia/ticket/{idMascota}", AVX, CLINICA, "Imprimir ticket de emergencia"));
        r.add(Regla.de("POST", "/api/cliente/vincular-mascota", C, CLINICA, "Vincular mascota de emergencia con código"));

        r.add(Regla.publica("GET", "/api/productos", COMERCIAL, "Ver catálogo de productos"));
        r.add(Regla.publica("GET", "/api/banners", COMERCIAL, "Ver banners de Farmacia o Accesorios"));
        r.add(Regla.de("GET", "/api/banners/gestion", AX, COMERCIAL, "Ver todos los banners de una sección"));
        r.add(Regla.de("POST", "/api/banners", X, COMERCIAL, "Subir banner"));
        r.add(Regla.de("PUT", "/api/banners/{id}", X, COMERCIAL, "Reemplazar imágenes de un banner"));
        r.add(Regla.de("PATCH", "/api/banners/{id}/orden", X, COMERCIAL, "Cambiar el orden de un banner"));
        r.add(Regla.de("PATCH", "/api/banners/{id}/activo", X, COMERCIAL, "Activar o desactivar un banner"));
        r.add(Regla.de("DELETE", "/api/banners/{id}", X, COMERCIAL, "Eliminar banner"));
        r.add(Regla.de("GET", "/api/productos/alertas", AX, COMERCIAL, "Ver alertas de stock y vencimiento"));
        r.add(Regla.publica("GET", "/api/productos/{id}", COMERCIAL, "Ver un producto"));
        r.add(Regla.de("POST", "/api/productos", X, COMERCIAL, "Crear producto"));
        r.add(Regla.de("PUT", "/api/productos/{id}", X, COMERCIAL, "Editar producto y precio"));
        r.add(Regla.de("DELETE", "/api/productos/{id}", X, COMERCIAL, "Dar de baja producto"));
        r.add(Regla.de("PATCH", "/api/productos/{id}/reactivar", X, COMERCIAL, "Reactivar producto"));
        r.add(Regla.publica("GET", "/api/producto-imagenes/producto/{productoId}", COMERCIAL, "Ver imágenes de un producto"));
        r.add(Regla.de("POST", "/api/producto-imagenes", X, COMERCIAL, "Agregar imagen de producto"));
        r.add(Regla.de("DELETE", "/api/producto-imagenes/{id}", X, COMERCIAL, "Quitar imagen de producto"));
        r.add(Regla.publica("GET", "/api/categorias", COMERCIAL, "Ver categorías"));
        r.add(Regla.de("POST", "/api/categorias", X, COMERCIAL, "Crear o editar categoría"));
        r.add(Regla.de("DELETE", "/api/categorias/{id}", X, COMERCIAL, "Dar de baja categoría"));
        r.add(Regla.de("PATCH", "/api/categorias/{id}/reactivar", X, COMERCIAL, "Reactivar categoría"));
        r.add(Regla.de("GET", "/api/lotes/producto/{productoId}", AX, COMERCIAL, "Ver lotes de un producto"));
        r.add(Regla.de("GET", "/api/lotes/por-vencer", AX, COMERCIAL, "Ver lotes por vencer"));
        r.add(Regla.de("GET", "/api/movimientos-stock/producto/{productoId}", AX, COMERCIAL, "Ver movimientos de stock"));
        r.add(Regla.de("POST", "/api/movimientos-stock", X, COMERCIAL, "Registrar entrada, salida o ajuste de stock"));
        r.add(Regla.de("GET", "/api/proveedores", AX, COMERCIAL, "Ver proveedores"));
        r.add(Regla.de("POST", "/api/proveedores", X, COMERCIAL, "Crear proveedor"));
        r.add(Regla.de("PUT", "/api/proveedores/{id}", X, COMERCIAL, "Editar proveedor"));
        r.add(Regla.de("DELETE", "/api/proveedores/{id}", X, COMERCIAL, "Dar de baja proveedor"));
        r.add(Regla.de("PATCH", "/api/proveedores/{id}/reactivar", X, COMERCIAL, "Reactivar proveedor"));
        r.add(Regla.de("GET", "/api/compras", AX, COMERCIAL, "Ver compras"));
        r.add(Regla.de("GET", "/api/compras/{id}/detalle", AX, COMERCIAL, "Ver detalle de compra"));
        r.add(Regla.de("POST", "/api/compras", X, COMERCIAL, "Registrar compra a proveedor"));
        r.add(Regla.de("GET", "/api/devoluciones", AX, COMERCIAL, "Ver devoluciones"));
        r.add(Regla.de("GET", "/api/devoluciones/{id}/detalle", AX, COMERCIAL, "Ver detalle de devolución"));
        r.add(Regla.de("GET", "/api/devoluciones/pedido/{pedidoId}/disponible", AX, COMERCIAL, "Ver qué se puede devolver de un pedido"));
        r.add(Regla.de("POST", "/api/devoluciones", X, COMERCIAL, "Registrar devolución"));
        r.add(Regla.de("GET", "/api/pedidos", AX, COMERCIAL, "Ver todos los pedidos"));
        r.add(Regla.propia("GET", "/api/pedidos/usuario/{usuarioId}", ACX, USUARIO, "usuarioId", COMERCIAL, "Ver pedidos de un cliente"));
        r.add(Regla.de("POST", "/api/pedidos", CX, COMERCIAL, "Comprar en la web (cliente) o vender en mostrador (auxiliar), con receta si el producto la pide"));
        r.add(Regla.de("PATCH", "/api/pedidos/{id}/estado", X, COMERCIAL, "Cambiar estado de pedido (al cancelarlo devuelve el stock y libera la receta)"));
        r.add(Regla.de("PATCH", "/api/pedidos/{id}/anular", X, COMERCIAL, "Anular una venta de mostrador que no se cobró (devuelve stock y libera la receta)"));
        r.add(Regla.propia("GET", "/api/pedido-detalle/pedido/{pedidoId}", ACX, PEDIDO, "pedidoId", COMERCIAL, "Ver productos de un pedido"));
        r.add(Regla.de("POST", "/api/pedido-detalle/validar-stock", CX, COMERCIAL, "Validar stock antes de vender"));
        r.add(Regla.de("GET", "/api/reportes/mas-vendidos", AX, COMERCIAL, "Ver productos más vendidos"));
        r.add(Regla.de("GET", "/api/reportes/valorizacion", AX, COMERCIAL, "Ver valorización del inventario"));
        r.add(Regla.de("POST", "/api/uploads", CX, GENERAL, "Subir imagen (receta del cliente, foto de producto)"));
        r.add(Regla.publica("POST", "/api/contacto", GENERAL, "Enviar mensaje de contacto"));
        r.add(Regla.de("GET", "/api/contacto", AX, GENERAL, "Ver bandeja de mensajes de contacto"));
        r.add(Regla.de("PATCH", "/api/contacto/{id}/responder", X, GENERAL, "Responder un mensaje de contacto por correo"));
        r.add(Regla.de("PATCH", "/api/contacto/{id}/archivar", X, GENERAL, "Archivar un mensaje de contacto"));

        r.add(Regla.de("GET", "/api/pagos", AX, COBROS, "Ver pagos"));
        r.add(Regla.propia("GET", "/api/pagos/usuario/{usuarioId}", ACX, USUARIO, "usuarioId", COBROS, "Ver pagos de un cliente"));
        r.add(Regla.de("GET", "/api/pagos/pendientes", ACX, COBROS, "Ver pagos por cobrar o por confirmar"));
        r.add(Regla.de("GET", "/api/pagos/resumen", AX, COBROS, "Ver resumen de caja"));
        r.add(Regla.de("POST", "/api/pagos", X, COBROS, "Registrar pago manual"));
        r.add(Regla.de("POST", "/api/pagos/registrar", CX, COBROS, "Pagar (cliente) o cobrar en mostrador (auxiliar)"));
        r.add(Regla.de("PATCH", "/api/pagos/{id}/yape", X, COBROS, "Confirmar o rechazar Yape"));
        r.add(Regla.de("GET", "/api/comprobantes", ACX, COBROS, "Ver comprobantes (cliente: solo los suyos)"));
        r.add(Regla.de("POST", "/api/comprobantes", X, COBROS, "Emitir comprobante"));
        r.add(Regla.propia("GET", "/api/comprobantes/por-pedido/{pedidoId}", ACX, PEDIDO, "pedidoId", COBROS, "Ver comprobante de un pedido"));
        r.add(Regla.de("GET", "/api/comprobantes/{id}/pdf", ACX, COBROS, "Descargar comprobante en PDF (cliente: solo los suyos)"));
        r.add(Regla.publica("GET", "/api/comprobantes/verificar/{codigo}", COBROS, "Verificar autenticidad de un comprobante"));
        r.add(Regla.de("GET", "/api/reportes/formal/{tipo}", A, COBROS, "Ver reportes de gestión"));
        r.add(Regla.de("GET", "/api/reportes/formal/{tipo}/pdf", A, COBROS, "Descargar reporte en PDF"));
        r.add(Regla.de("GET", "/api/reportes/formal/{tipo}/xlsx", A, COBROS, "Descargar reporte en Excel"));

        r.add(Regla.publica("GET", "/api/hospedaje/configuracion", ESTANCIAS, "Ver precios y horario de hospedaje"));
        r.add(Regla.de("GET", "/api/hospedaje/disponibilidad", ACX, ESTANCIAS, "Ver cuartos libres"));
        r.add(Regla.de("POST", "/api/cliente/hospedaje/reservar", C, ESTANCIAS, "Reservar hospedaje"));
        r.add(Regla.de("GET", "/api/estancias", TODOS, ESTANCIAS, "Ver estancias (cliente: solo las suyas)"));
        r.add(Regla.de("GET", "/api/estancias/cuartos", AVX, ESTANCIAS, "Ver estado de los cuartos"));
        r.add(Regla.de("POST", "/api/veterinario/internamientos", V, ESTANCIAS, "Internar mascota operada"));
        r.add(Regla.de("PATCH", "/api/estancias/{id}/ingreso", VX, ESTANCIAS, "Registrar ingreso (auxiliar: hospedaje; veterinario: internamiento)"));
        r.add(Regla.de("PATCH", "/api/estancias/{id}/salida", VX, ESTANCIAS, "Registrar salida (auxiliar: hospedaje; veterinario: alta de internamiento)"));
        r.add(Regla.de("PATCH", "/api/estancias/{id}/cancelar", X, ESTANCIAS, "Cancelar reserva de hospedaje"));
        r.add(Regla.publica("GET", "/api/viajes/tarifa", ESTANCIAS, "Ver tarifa de viajes"));
        r.add(Regla.publica("GET", "/api/viajes/requisitos", ESTANCIAS, "Ver documentos que pide un viaje"));
        r.add(Regla.de("POST", "/api/cliente/viajes/solicitar", C, ESTANCIAS, "Solicitar viaje con documento"));
        r.add(Regla.de("POST", "/api/cliente/viajes/{id}/documentos", C, ESTANCIAS, "Reenviar documentos observados de mi viaje"));
        r.add(Regla.de("GET", "/api/viajes", ACX, ESTANCIAS, "Ver solicitudes de viaje (cliente: solo las suyas)"));
        r.add(Regla.de("PATCH", "/api/viajes/{id}/revisar", X, ESTANCIAS, "Aprobar o rechazar documento de viaje"));
        r.add(Regla.de("GET", "/api/viajes/{id}/documento", ACX, ESTANCIAS, "Descargar documento de viaje"));
        r.add(Regla.de("GET", "/api/viajes/{id}/documentos/{docId}", ACX, ESTANCIAS, "Descargar un documento de viaje por requisito"));

        r.add(Regla.publica("GET", "/api/servicios", CATALOGO, "Ver servicios y precios"));
        r.add(Regla.publica("GET", "/api/configuracion", CATALOGO, "Ver datos de contacto y horarios de la clínica"));
        r.add(Regla.de("PUT", "/api/configuracion", A, CATALOGO, "Cambiar la configuración de la clínica"));
        r.add(Regla.de("POST", "/api/servicios", A, CATALOGO, "Crear o editar servicio y precio"));
        r.add(Regla.de("DELETE", "/api/servicios/{id}", A, CATALOGO, "Dar de baja servicio"));
        r.add(Regla.de("PATCH", "/api/servicios/{id}/reactivar", A, CATALOGO, "Reactivar servicio"));
        r.add(Regla.publica("GET", "/api/especialidades", CATALOGO, "Ver especialidades"));
        r.add(Regla.de("POST", "/api/especialidades", A, CATALOGO, "Crear o editar especialidad"));
        r.add(Regla.de("DELETE", "/api/especialidades/{id}", A, CATALOGO, "Dar de baja especialidad"));
        r.add(Regla.de("PATCH", "/api/especialidades/{id}/reactivar", A, CATALOGO, "Reactivar especialidad"));
        r.add(Regla.de("GET", "/api/horarios", AVX, CATALOGO, "Ver horarios de atención"));
        r.add(Regla.de("POST", "/api/horarios", A, CATALOGO, "Definir horario de veterinario"));
        r.add(Regla.de("DELETE", "/api/horarios/{id}", A, CATALOGO, "Quitar horario de veterinario"));
        r.add(Regla.de("GET", "/api/veterinarios/{id}/especialidades", AVX, CATALOGO, "Ver especialidades de un veterinario"));
        r.add(Regla.de("PUT", "/api/veterinarios/{id}/especialidades", A, CATALOGO, "Asignar especialidades a un veterinario"));
        r.add(Regla.publica("GET", "/api/blog", CATALOGO, "Ver artículos del blog"));
        r.add(Regla.de("POST", "/api/blog", A, CATALOGO, "Publicar artículo"));
        r.add(Regla.de("DELETE", "/api/blog/{id}", A, CATALOGO, "Ocultar artículo"));
        r.add(Regla.de("PATCH", "/api/blog/{id}/reactivar", A, CATALOGO, "Reactivar artículo"));

        r.add(Regla.propiaParaTodos("GET", "/api/notificaciones/usuario/{usuarioId}", TODOS, USUARIO, "usuarioId", GENERAL, "Ver mis notificaciones"));
        r.add(Regla.propiaParaTodos("PATCH", "/api/notificaciones/usuario/{usuarioId}/leer-todas", TODOS, USUARIO, "usuarioId", GENERAL, "Marcar mis notificaciones como leídas"));
        r.add(Regla.de("PATCH", "/api/notificaciones/{id}/leer", TODOS, GENERAL, "Marcar una notificación como leída (solo propias)"));
        r.add(Regla.de("POST", "/api/notificaciones", A, GENERAL, "Enviar aviso manual"));
        r.add(Regla.de("POST", "/api/asistente-ia", TODOS, GENERAL, "Usar el asistente virtual"));

        REGLAS = Collections.unmodifiableList(r);
    }

    private PermisosApi() {
    }

    public static List<Regla> reglas() {
        return REGLAS;
    }

    public record Coincidencia(Regla regla, Map<String, String> variables) {
    }

    public static Coincidencia buscar(String metodo, String ruta) {
        for (Regla regla : REGLAS) {
            Map<String, String> variables = regla.coincide(metodo, ruta);
            if (variables != null) return new Coincidencia(regla, variables);
        }
        return null;
    }
}

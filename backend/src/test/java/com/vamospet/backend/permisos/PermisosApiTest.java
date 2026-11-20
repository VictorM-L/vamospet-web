package com.vamospet.backend.permisos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PermisosApiTest {

    static Stream<Arguments> anexoA() {
        return Stream.of(
                Arguments.of("Gestionar personal", "POST", "/api/usuarios/personal", true, false, false, false),
                Arguments.of("Restablecer contraseña del personal", "POST", "/api/usuarios/personal/7/restablecimiento", true, false, false, false),
                Arguments.of("Servicios y precios", "POST", "/api/servicios", true, false, false, false),
                Arguments.of("Horarios", "POST", "/api/horarios", true, false, false, false),
                Arguments.of("Agendar", "POST", "/api/citas", false, false, false, true),
                Arguments.of("Comprar en la web o vender en mostrador", "POST", "/api/pedidos", false, false, true, true),
                Arguments.of("Confirmar cita", "PATCH", "/api/citas/5/confirmar", false, true, false, false),
                Arguments.of("No puedo atender", "PATCH", "/api/citas/5/no-puedo-atender", false, true, false, false),
                Arguments.of("Rechazar, cancelar, No asistió", "PATCH", "/api/citas/5/estado", false, false, true, false),
                Arguments.of("Reasignar", "PATCH", "/api/citas/5/reasignar", false, false, true, false),
                Arguments.of("Cliente cancela con 24 h", "PATCH", "/api/citas/5/cancelar", false, false, false, true),
                Arguments.of("Cliente solicita cancelación", "POST", "/api/citas/5/solicitud-cancelacion", false, false, false, true),
                Arguments.of("Atender", "POST", "/api/citas/5/atender", false, true, false, false),
                Arguments.of("Recetas: aprobar", "PATCH", "/api/recetas/5/estado", false, true, false, false),
                Arguments.of("Exámenes: solicitar", "POST", "/api/veterinario/examenes/solicitar", false, true, false, false),
                Arguments.of("Cirugías: programar", "POST", "/api/veterinario/cirugias/agendar", false, true, false, false),
                Arguments.of("Internamiento", "POST", "/api/veterinario/internamientos", false, true, false, false),
                Arguments.of("Historial (lectura)", "GET", "/api/historial/mascota/5", true, true, false, true),
                Arguments.of("Costos adicionales", "POST", "/api/citas/5/costos-adicionales", false, true, false, false),
                Arguments.of("Cobrar cita", "POST", "/api/citas/5/cobrar", false, false, true, false),
                Arguments.of("Confirmar Yape", "PATCH", "/api/pagos/5/yape", false, false, true, false),
                Arguments.of("Cobros (lectura)", "GET", "/api/pagos", true, false, true, false),
                Arguments.of("Inventario: movimiento", "POST", "/api/movimientos-stock", false, false, true, false),
                Arguments.of("Inventario: precio de producto", "PUT", "/api/productos/5", false, false, true, false),
                Arguments.of("Compras", "POST", "/api/compras", false, false, true, false),
                Arguments.of("Compras (lectura)", "GET", "/api/compras", true, false, true, false),
                Arguments.of("Devoluciones", "POST", "/api/devoluciones", false, false, true, false),
                Arguments.of("Pedidos: estado", "PATCH", "/api/pedidos/5/estado", false, false, true, false),
                Arguments.of("Clientes sin cuenta", "POST", "/api/usuarios/cliente-sin-cuenta", false, false, true, false),
                Arguments.of("Invitar a crear cuenta", "POST", "/api/usuarios/5/invitacion", false, false, true, false),
                Arguments.of("Hospedaje: ingreso", "PATCH", "/api/estancias/5/ingreso", false, true, true, false),
                Arguments.of("Hospedaje: cancelar", "PATCH", "/api/estancias/5/cancelar", false, false, true, false),
                Arguments.of("Revisar documento de viaje", "PATCH", "/api/viajes/5/revisar", false, false, true, false),
                Arguments.of("Registrar emergencia", "POST", "/api/auxiliar/emergencia/registrar", false, false, true, false),
                Arguments.of("Subir resultado de examen (trámite)", "POST", "/api/auxiliar/examenes/subir", false, false, true, false),
                Arguments.of("Reportes formales", "GET", "/api/reportes/formal/ingresos", true, false, false, false),
                Arguments.of("Ver todas las citas", "GET", "/api/citas", true, true, true, false),
                Arguments.of("Lista de usuarios", "GET", "/api/usuarios", true, true, true, false)
        );
    }

    @ParameterizedTest(name = "{0}: Admin={3} Vet={4} Aux={5} Cliente={6}")
    @MethodSource("anexoA")
    @DisplayName("La matriz cumple el Anexo A")
    void cumpleAnexoA(String funcion, String metodo, String ruta, boolean admin, boolean vet, boolean aux, boolean cliente) {
        PermisosApi.Coincidencia c = PermisosApi.buscar(metodo, ruta);
        assertNotNull(c, "Sin regla para " + metodo + " " + ruta);
        assertEquals(admin, c.regla().permite(Rol.ADMIN), funcion + " / Admin");
        assertEquals(vet, c.regla().permite(Rol.VETERINARIO), funcion + " / Veterinario");
        assertEquals(aux, c.regla().permite(Rol.AUXILIAR), funcion + " / Auxiliar");
        assertEquals(cliente, c.regla().permite(Rol.CLIENTE), funcion + " / Cliente");
    }

    @Test
    @DisplayName("El Administrador nunca escribe en inventario, citas, cobros ni historial clínico")
    void adminSoloLectura() {
        for (Regla r : PermisosApi.reglas()) {
            if (r.isPublica() || "GET".equals(r.getMetodo())) continue;
            boolean areaRestringida = r.getArea().equals(PermisosApi.CLINICA) || r.getArea().equals(PermisosApi.CITAS)
                    || r.getArea().equals(PermisosApi.COBROS) || r.getArea().equals(PermisosApi.COMERCIAL)
                    || r.getArea().equals(PermisosApi.ESTANCIAS);
            if (areaRestringida) assertFalse(r.permite(Rol.ADMIN), "El Admin no debería poder: " + r.getMetodo() + " " + r.getPatron());
        }
    }

    @Test
    @DisplayName("El Veterinario no cobra ni toca dinero")
    void veterinarioSinDinero() {
        for (Regla r : PermisosApi.reglas()) {
            if (r.getArea().equals(PermisosApi.COBROS) && !r.isPublica()) {
                assertFalse(r.permite(Rol.VETERINARIO), "El veterinario no debería poder: " + r.getMetodo() + " " + r.getPatron());
            }
        }
    }

    @Test
    @DisplayName("No hay reglas repetidas")
    void sinRepetidas() {
        Set<String> vistas = new HashSet<>();
        for (Regla r : PermisosApi.reglas()) {
            assertTrue(vistas.add(r.getMetodo() + " " + r.getPatron()), "Regla repetida: " + r.getMetodo() + " " + r.getPatron());
            assertFalse(r.getFuncion().isBlank());
        }
    }

    @Test
    @DisplayName("Una ruta específica gana a la genérica")
    void especificaPrimero() {
        assertEquals("/api/productos/alertas", PermisosApi.buscar("GET", "/api/productos/alertas").regla().getPatron());
        assertEquals("/api/productos/{id}", PermisosApi.buscar("GET", "/api/productos/12").regla().getPatron());
        assertTrue(PermisosApi.buscar("GET", "/api/productos/12").regla().isPublica());
    }

    @Test
    @DisplayName("Lo que no está en la matriz no tiene regla (se rechaza por defecto)")
    void desconocidaSinRegla() {
        assertNull(PermisosApi.buscar("GET", "/api/inventado"));
        assertNull(PermisosApi.buscar("DELETE", "/api/citas/5"));
    }

    @Test
    @DisplayName("La propiedad se exige al cliente, y en notificaciones a todos los roles")
    void propiedad() {
        Regla mascotas = PermisosApi.buscar("GET", "/api/mascotas/usuario/3").regla();
        assertTrue(mascotas.exigePropiedad(Rol.CLIENTE));
        assertFalse(mascotas.exigePropiedad(Rol.VETERINARIO));
        Regla notificaciones = PermisosApi.buscar("GET", "/api/notificaciones/usuario/3").regla();
        assertTrue(notificaciones.exigePropiedad(Rol.ADMIN));
        assertTrue(notificaciones.exigePropiedad(Rol.CLIENTE));
    }
}

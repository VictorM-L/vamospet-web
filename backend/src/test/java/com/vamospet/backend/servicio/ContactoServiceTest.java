package com.vamospet.backend.servicio;

import com.vamospet.backend.dto.ContactoRequest;
import com.vamospet.backend.modelo.MensajeContacto;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.MensajeContactoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

    private static final int AUX = 19;

    @Mock private MensajeContactoRepository repositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private AutorizacionService autorizacion;
    @Mock private NotificacionService notificaciones;
    @InjectMocks private ContactoService servicio;

    private ContactoRequest pedido(String correo, String trampa) {
        return new ContactoRequest("Ana Torres", correo, "987654321", "Otro", "Quisiera saber los horarios del sábado.", trampa);
    }

    private MensajeContacto mensaje(String estado) {
        MensajeContacto m = new MensajeContacto();
        m.setId(3);
        m.setNombre("Ana Torres");
        m.setCorreo("ana@correo.pe");
        m.setAsunto("Consulta sobre farmacia");
        m.setMensaje("¿Tienen antipulgas?\nPara un gato de 4 kg.");
        m.setEstado(estado);
        m.setCreado_en(LocalDateTime.of(2026, 11, 10, 9, 30));
        return m;
    }

    private void comoAuxiliar() {
        Usuario u = new Usuario();
        u.setId(AUX);
        u.setRol_id(4);
        when(autorizacion.conRol(AUX, 4)).thenReturn(u);
    }

    @Test
    @DisplayName("Un mensaje válido se guarda como Nuevo y avisa a los auxiliares")
    void recibe() {
        Usuario aux = new Usuario();
        aux.setId(AUX);
        aux.setRol_id(4);
        aux.setActivo(true);
        when(usuarioRepositorio.findAll()).thenReturn(List.of(aux));
        when(repositorio.save(any(MensajeContacto.class))).thenAnswer(i -> i.getArgument(0));
        MensajeContacto m = servicio.recibir(pedido("Ana@Correo.pe", null), "10.0.0.5");
        assertEquals("Nuevo", m.getEstado());
        assertEquals("ana@correo.pe", m.getCorreo());
        assertEquals("10.0.0.5", m.getIp_origen());
        verify(notificaciones).notificar(eq(AUX), eq("Nuevo mensaje de contacto"), anyString(), eq("Contacto"));
    }

    @Test
    @DisplayName("Con el campo trampa lleno (bot) no se guarda nada")
    void trampaParaBots() {
        servicio.recibir(pedido("bot@spam.com", "http://spam"), "10.0.0.9");
        verify(repositorio, never()).save(any());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("Más de 5 mensajes por hora desde la misma conexión se rechazan (429)")
    void limitePorIp() {
        when(repositorio.contarPorIpDesde(eq("10.0.0.5"), any())).thenReturn(5L);
        NegocioException ex = assertThrows(NegocioException.class, () -> servicio.recibir(pedido("otro@correo.pe", null), "10.0.0.5"));
        assertEquals(429, ex.getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Más de 3 mensajes por hora con el mismo correo se rechazan (429)")
    void limitePorCorreo() {
        when(repositorio.contarPorIpDesde(eq("10.0.0.6"), any())).thenReturn(0L);
        when(repositorio.contarPorCorreoDesde(eq("ana@correo.pe"), any())).thenReturn(3L);
        assertEquals(429, assertThrows(NegocioException.class, () -> servicio.recibir(pedido("ana@correo.pe", null), "10.0.0.6")).getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("La respuesta lleva asunto Re: y cita el mensaje original")
    void correoConCita() {
        MensajeContacto m = mensaje("Nuevo");
        assertEquals("Re: Consulta sobre farmacia", ContactoService.asuntoRespuesta(m));
        String texto = ContactoService.textoRespuesta(m, "Sí, tenemos pipetas para gatos.");
        assertTrue(texto.startsWith("Hola Ana Torres,"));
        assertTrue(texto.contains("Sí, tenemos pipetas para gatos."));
        assertTrue(texto.contains("El 10/11/2026 09:30, Ana Torres escribió:"));
        assertTrue(texto.contains("> ¿Tienen antipulgas?\n> Para un gato de 4 kg."));
    }

    @Test
    @DisplayName("Responder envía el correo y deja el mensaje como Respondido")
    void responder() {
        comoAuxiliar();
        MensajeContacto m = mensaje("Nuevo");
        when(repositorio.findById(3)).thenReturn(Optional.of(m));
        when(notificaciones.enviarCorreo(eq("ana@correo.pe"), eq("Re: Consulta sobre farmacia"), anyString(), any(), any())).thenReturn(true);
        when(repositorio.save(any(MensajeContacto.class))).thenAnswer(i -> i.getArgument(0));
        MensajeContacto r = servicio.responder(AUX, 3, "Sí, tenemos pipetas para gatos.");
        assertEquals("Respondido", r.getEstado());
        assertEquals(AUX, r.getRespondido_por());
        assertNotNull(r.getRespondido_en());
    }

    @Test
    @DisplayName("Si el correo no sale, el mensaje sigue Nuevo y no se guarda la respuesta")
    void correoFallido() {
        comoAuxiliar();
        MensajeContacto m = mensaje("Nuevo");
        when(repositorio.findById(3)).thenReturn(Optional.of(m));
        when(notificaciones.enviarCorreo(anyString(), anyString(), anyString(), any(), any())).thenReturn(false);
        assertEquals(502, assertThrows(NegocioException.class, () -> servicio.responder(AUX, 3, "Sí, tenemos.")).getEstado());
        assertEquals("Nuevo", m.getEstado());
        assertNull(m.getRespuesta());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("No se responde dos veces ni un mensaje archivado; respuesta vacía no vale")
    void estados() {
        comoAuxiliar();
        when(repositorio.findById(3)).thenReturn(Optional.of(mensaje("Respondido")));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.responder(AUX, 3, "Otra vez")).getEstado());
        when(repositorio.findById(3)).thenReturn(Optional.of(mensaje("Archivado")));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.responder(AUX, 3, "Otra vez")).getEstado());
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.archivar(AUX, 3)).getEstado());
        when(repositorio.findById(3)).thenReturn(Optional.of(mensaje("Nuevo")));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.responder(AUX, 3, "  ")).getEstado());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("Archivar deja el mensaje como Archivado")
    void archivar() {
        comoAuxiliar();
        when(repositorio.findById(3)).thenReturn(Optional.of(mensaje("Respondido")));
        when(repositorio.save(any(MensajeContacto.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("Archivado", servicio.archivar(AUX, 3).getEstado());
    }

    @Test
    @DisplayName("Solo el Auxiliar responde; el Admin solo lee la bandeja")
    void permisos() {
        when(autorizacion.conRol(1, 4)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.responder(1, 3, "Hola, gracias")).getEstado());
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.archivar(1, 3)).getEstado());
        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setRol_id(1);
        when(autorizacion.conRol(1, 1, 4)).thenReturn(admin);
        when(repositorio.recientes()).thenReturn(List.of(mensaje("Nuevo"), mensaje("Respondido")));
        assertEquals(1, servicio.listar(1, "Respondido").size());
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.listar(1, "Borrado")).getEstado());
        when(autorizacion.conRol(2, 1, 4)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.listar(2, null)).getEstado());
    }
}

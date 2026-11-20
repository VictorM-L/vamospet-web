package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.CodigoVerificacion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionServiceTest {

    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private CodigoService codigos;
    @Mock private ClaveService claves;
    @Mock private NotificacionService notificaciones;
    @Mock private AutorizacionService autorizacion;
    @InjectMocks private RecuperacionService servicio;

    private Usuario usuario(int id, int rol, String correo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        u.setNombre("Ana Ruiz");
        u.setCorreo(correo);
        u.setActivo(true);
        return u;
    }

    @Test
    @DisplayName("Respuesta genérica: un correo que no existe no genera código ni envía nada")
    void correoInexistente() {
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("nadie@correo.pe")).thenReturn(List.of());
        assertDoesNotThrow(() -> servicio.solicitar("nadie@correo.pe"));
        verifyNoInteractions(codigos, notificaciones);
    }

    @Test
    @DisplayName("Respuesta genérica: una cuenta dada de baja se trata como inexistente")
    void cuentaDeBaja() {
        Usuario u = usuario(5, 2, "ana@correo.pe");
        u.setActivo(false);
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("ana@correo.pe")).thenReturn(List.of(u));
        servicio.solicitar("ana@correo.pe");
        verifyNoInteractions(codigos, notificaciones);
    }

    @Test
    @DisplayName("Con correo válido genera un código de 15 minutos y lo envía por correo")
    void solicitudEnviaCodigo() {
        Usuario u = usuario(5, 2, "ana@correo.pe");
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("ana@correo.pe")).thenReturn(List.of(u));
        when(codigos.enviadosDesde(eq(5), eq(CodigoService.RECUPERACION), any())).thenReturn(0L);
        when(codigos.generar(5, CodigoService.RECUPERACION, 15)).thenReturn("482913");
        servicio.solicitar(" ana@correo.pe ");
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).enviarCorreoDirecto(eq("ana@correo.pe"), anyString(), texto.capture());
        assertTrue(texto.getValue().contains("482913"));
        assertTrue(texto.getValue().contains("15 minutos"));
    }

    @Test
    @DisplayName("Límite de envíos: con 3 códigos en 15 minutos no se genera un cuarto")
    void limiteDeEnvios() {
        Usuario u = usuario(5, 2, "ana@correo.pe");
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("ana@correo.pe")).thenReturn(List.of(u));
        when(codigos.enviadosDesde(eq(5), eq(CodigoService.RECUPERACION), any())).thenReturn(3L);
        servicio.solicitar("ana@correo.pe");
        verify(codigos, never()).generar(anyInt(), anyString(), anyInt());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("Verificar con un correo sin cuenta responde 400 sin revelar nada")
    void verificarSinCuenta() {
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("nadie@correo.pe")).thenReturn(List.of());
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.verificar("nadie@correo.pe", "123456"));
        assertEquals(400, e.getEstado());
        verifyNoInteractions(codigos);
    }

    @Test
    @DisplayName("Restablecer guarda la nueva contraseña, marca el código como usado y avisa por correo")
    void restablecer() {
        Usuario u = usuario(5, 2, "ana@correo.pe");
        CodigoVerificacion c = new CodigoVerificacion();
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("ana@correo.pe")).thenReturn(List.of(u));
        when(codigos.validar(5, CodigoService.RECUPERACION, "123456")).thenReturn(c);
        when(claves.codificar("NuevaClave1")).thenReturn("NuevaClave1");
        servicio.restablecer("ana@correo.pe", "123456", "NuevaClave1");
        verify(codigos).marcarUsado(c);
        verify(usuarioRepositorio).save(u);
        assertEquals("NuevaClave1", u.getContrasena());
        verify(notificaciones).enviarCorreoDirecto(eq("ana@correo.pe"), eq("Tu contraseña fue cambiada"), anyString());
    }

    @Test
    @DisplayName("Si el código no es válido la contraseña no cambia")
    void restablecerCodigoMalo() {
        Usuario u = usuario(5, 2, "ana@correo.pe");
        u.setContrasena("Antigua1");
        when(usuarioRepositorio.findAllByCorreoIgnoreCase("ana@correo.pe")).thenReturn(List.of(u));
        when(codigos.validar(5, CodigoService.RECUPERACION, "000000")).thenThrow(new NegocioException(400, "Código incorrecto."));
        assertThrows(NegocioException.class, () -> servicio.restablecer("ana@correo.pe", "000000", "NuevaClave1"));
        assertEquals("Antigua1", u.getContrasena());
        verify(usuarioRepositorio, never()).save(any());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("Solo el Admin envía restablecimiento al personal (403 para otros roles)")
    void adminSolamente() {
        when(autorizacion.conRol(4, AutorizacionService.ADMIN)).thenReturn(null);
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.enviarRestablecimientoPorAdmin(4, 18));
        assertEquals(403, e.getEstado());
        verifyNoInteractions(codigos, notificaciones);
    }

    @Test
    @DisplayName("Restablecimiento a un colaborador inexistente responde 404")
    void colaboradorInexistente() {
        when(autorizacion.conRol(1, AutorizacionService.ADMIN)).thenReturn(usuario(1, 1, "admin@v.pe"));
        when(usuarioRepositorio.findById(99)).thenReturn(Optional.empty());
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.enviarRestablecimientoPorAdmin(1, 99));
        assertEquals(404, e.getEstado());
    }

    @Test
    @DisplayName("El botón del Admin no sirve para clientes")
    void noParaClientes() {
        when(autorizacion.conRol(1, AutorizacionService.ADMIN)).thenReturn(usuario(1, 1, "admin@v.pe"));
        when(usuarioRepositorio.findById(20)).thenReturn(Optional.of(usuario(20, 2, "cli@v.pe")));
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.enviarRestablecimientoPorAdmin(1, 20));
        assertEquals(409, e.getEstado());
        verifyNoInteractions(codigos);
    }

    @Test
    @DisplayName("El Admin recibe 429 si ya se enviaron 3 códigos en 15 minutos")
    void adminLimite() {
        when(autorizacion.conRol(1, AutorizacionService.ADMIN)).thenReturn(usuario(1, 1, "admin@v.pe"));
        when(usuarioRepositorio.findById(18)).thenReturn(Optional.of(usuario(18, 3, "vet@v.pe")));
        when(codigos.enviadosDesde(eq(18), eq(CodigoService.RECUPERACION), any())).thenReturn(3L);
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.enviarRestablecimientoPorAdmin(1, 18));
        assertEquals(429, e.getEstado());
        verify(codigos, never()).generar(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("El Admin envía el código al correo del veterinario y nunca ve la contraseña")
    void adminEnvia() {
        when(autorizacion.conRol(1, AutorizacionService.ADMIN)).thenReturn(usuario(1, 1, "admin@v.pe"));
        when(usuarioRepositorio.findById(18)).thenReturn(Optional.of(usuario(18, 3, "vet@v.pe")));
        when(codigos.enviadosDesde(eq(18), eq(CodigoService.RECUPERACION), any())).thenReturn(0L);
        when(codigos.generar(18, CodigoService.RECUPERACION, 15)).thenReturn("777111");
        when(notificaciones.enviarCorreoDirecto(eq("vet@v.pe"), anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(() -> servicio.enviarRestablecimientoPorAdmin(1, 18));
        verifyNoInteractions(claves);
    }

    @Test
    @DisplayName("Si el correo no sale, el Admin recibe 503")
    void adminCorreoFalla() {
        when(autorizacion.conRol(1, AutorizacionService.ADMIN)).thenReturn(usuario(1, 1, "admin@v.pe"));
        when(usuarioRepositorio.findById(18)).thenReturn(Optional.of(usuario(18, 3, "vet@v.pe")));
        when(codigos.enviadosDesde(eq(18), eq(CodigoService.RECUPERACION), any())).thenReturn(0L);
        when(codigos.generar(18, CodigoService.RECUPERACION, 15)).thenReturn("777111");
        when(notificaciones.enviarCorreoDirecto(eq("vet@v.pe"), anyString(), anyString())).thenReturn(false);
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.enviarRestablecimientoPorAdmin(1, 18));
        assertEquals(503, e.getEstado());
    }
}

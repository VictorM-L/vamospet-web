package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.CodigoVerificacion;
import com.vamospet.backend.repositorio.CodigoVerificacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodigoServiceTest {

    @Mock private CodigoVerificacionRepository repositorio;
    @InjectMocks private CodigoService servicio;

    private CodigoVerificacion codigo(String plano, LocalDateTime expira, int intentos, boolean usado) {
        CodigoVerificacion c = new CodigoVerificacion();
        c.setId(1);
        c.setUsuario_id(7);
        c.setProposito(CodigoService.RECUPERACION);
        c.setCodigo_hash(servicio.hash(7, plano));
        c.setExpira(expira);
        c.setIntentos(intentos);
        c.setUsado(usado);
        return c;
    }

    private void ultimoEs(CodigoVerificacion c) {
        when(repositorio.buscarRecientes(7, CodigoService.RECUPERACION)).thenReturn(List.of(c));
    }

    @Test
    @DisplayName("Generar guarda el hash (nunca el código en claro), vence en 15 minutos e invalida los anteriores")
    void generarGuardaHash() {
        CodigoVerificacion anterior = codigo("111111", LocalDateTime.now().plusMinutes(5), 0, false);
        ultimoEs(anterior);
        String plano = servicio.generar(7, CodigoService.RECUPERACION, 15);
        assertTrue(plano.matches("\\d{6}"));
        assertTrue(anterior.getUsado());
        ArgumentCaptor<CodigoVerificacion> captor = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(repositorio, times(2)).save(captor.capture());
        CodigoVerificacion nuevo = captor.getAllValues().get(1);
        assertNotEquals(plano, nuevo.getCodigo_hash());
        assertEquals(servicio.hash(7, plano), nuevo.getCodigo_hash());
        assertEquals(CodigoService.RECUPERACION, nuevo.getProposito());
        assertFalse(nuevo.getUsado());
        assertEquals(15, java.time.Duration.between(nuevo.getCreado(), nuevo.getExpira()).toMinutes());
    }

    @Test
    @DisplayName("Un código vencido es rechazado con 410")
    void codigoExpirado() {
        ultimoEs(codigo("123456", LocalDateTime.now().minusSeconds(1), 0, false));
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.validar(7, CodigoService.RECUPERACION, "123456"));
        assertEquals(410, e.getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Un código ya usado no sirve otra vez")
    void codigoUsado() {
        ultimoEs(codigo("123456", LocalDateTime.now().plusMinutes(10), 0, true));
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.validar(7, CodigoService.RECUPERACION, "123456"));
        assertEquals(410, e.getEstado());
    }

    @Test
    @DisplayName("Sin ningún código pedido responde 410")
    void sinCodigo() {
        when(repositorio.buscarRecientes(7, CodigoService.RECUPERACION)).thenReturn(List.of());
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.validar(7, CodigoService.RECUPERACION, "123456"));
        assertEquals(410, e.getEstado());
    }

    @Test
    @DisplayName("Un código incorrecto suma un intento y avisa cuántos quedan")
    void codigoIncorrecto() {
        CodigoVerificacion c = codigo("123456", LocalDateTime.now().plusMinutes(10), 0, false);
        ultimoEs(c);
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.validar(7, CodigoService.RECUPERACION, "000000"));
        assertEquals(400, e.getEstado());
        assertTrue(e.getMessage().contains("4 intentos"));
        assertEquals(1, c.getIntentos());
        verify(repositorio).save(c);
    }

    @Test
    @DisplayName("El quinto fallo bloquea el código con 429")
    void quintoFallo() {
        CodigoVerificacion c = codigo("123456", LocalDateTime.now().plusMinutes(10), 4, false);
        ultimoEs(c);
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.validar(7, CodigoService.RECUPERACION, "000000"));
        assertEquals(429, e.getEstado());
        assertEquals(5, c.getIntentos());
    }

    @Test
    @DisplayName("El sexto intento es rechazado aunque el código sea el correcto")
    void sextoIntento() {
        CodigoVerificacion c = codigo("123456", LocalDateTime.now().plusMinutes(10), 5, false);
        ultimoEs(c);
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.validar(7, CodigoService.RECUPERACION, "123456"));
        assertEquals(429, e.getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("El código correcto y vigente se acepta sin sumar intentos")
    void codigoCorrecto() {
        CodigoVerificacion c = codigo("123456", LocalDateTime.now().plusMinutes(10), 2, false);
        ultimoEs(c);
        assertSame(c, servicio.validar(7, CodigoService.RECUPERACION, " 123456 "));
        assertEquals(2, c.getIntentos());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("El hash depende del usuario: el mismo código no sirve para otra cuenta")
    void hashPorUsuario() {
        assertNotEquals(servicio.hash(7, "123456"), servicio.hash(8, "123456"));
    }
}

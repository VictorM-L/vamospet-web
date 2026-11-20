package com.vamospet.backend.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class IntentosLoginServiceTest {

    private static final class RelojMovible extends Clock {
        private Instant ahora = Instant.parse("2026-11-18T20:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojMovible reloj = new RelojMovible();
    private final IntentosLoginService intentos = new IntentosLoginService(reloj);

    @Test
    @DisplayName("El quinto intento fallido bloquea el correo 15 minutos; los anteriores avisan cuántos quedan")
    void bloqueaAlQuinto() {
        assertEquals(4, intentos.registrarFallo("ana@correo.pe"));
        assertEquals(3, intentos.registrarFallo("ANA@correo.pe "));
        assertEquals(2, intentos.registrarFallo("ana@correo.pe"));
        assertEquals(1, intentos.registrarFallo("ana@correo.pe"));
        assertNull(intentos.bloqueoRestante("ana@correo.pe"));
        assertEquals(0, intentos.registrarFallo("ana@correo.pe"));
        assertEquals(Duration.ofMinutes(15), intentos.bloqueoRestante("ana@correo.pe"));
        assertNull(intentos.bloqueoRestante("otra@correo.pe"));
    }

    @Test
    @DisplayName("El bloqueo termina a los 15 minutos y el conteo vuelve a empezar")
    void bloqueoTermina() {
        for (int i = 0; i < 5; i++) intentos.registrarFallo("ana@correo.pe");
        reloj.avanzar(Duration.ofMinutes(14).plusSeconds(59));
        assertEquals(Duration.ofSeconds(1), intentos.bloqueoRestante("ana@correo.pe"));
        reloj.avanzar(Duration.ofSeconds(1));
        assertNull(intentos.bloqueoRestante("ana@correo.pe"));
        assertEquals(4, intentos.registrarFallo("ana@correo.pe"));
    }

    @Test
    @DisplayName("Fallos separados por más de 15 minutos no se suman, y un ingreso correcto borra el conteo")
    void ventanaYLimpieza() {
        intentos.registrarFallo("ana@correo.pe");
        intentos.registrarFallo("ana@correo.pe");
        reloj.avanzar(Duration.ofMinutes(16));
        assertEquals(4, intentos.registrarFallo("ana@correo.pe"));
        intentos.registrarFallo("ana@correo.pe");
        intentos.limpiar("Ana@Correo.pe");
        assertEquals(4, intentos.registrarFallo("ana@correo.pe"));
    }

    @Test
    @DisplayName("Los mensajes dicen el tiempo restante y los intentos que quedan")
    void mensajes() {
        assertEquals("Correo o contraseña incorrectos.", IntentosLoginService.mensajeFallo(3));
        assertTrue(IntentosLoginService.mensajeFallo(2).contains("Te quedan 2 intentos antes de un bloqueo de 15 minutos"));
        assertTrue(IntentosLoginService.mensajeFallo(1).contains("Te queda 1 intento"));
        assertTrue(IntentosLoginService.mensajeBloqueo(Duration.ofMinutes(15)).contains("en 15 minutos"));
        assertTrue(IntentosLoginService.mensajeBloqueo(Duration.ofSeconds(20)).contains("en 1 minuto "));
        assertTrue(IntentosLoginService.mensajeBloqueo(Duration.ofSeconds(61)).contains("en 2 minutos"));
    }
}

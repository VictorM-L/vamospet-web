package com.vamospet.backend.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres-123";
    private static final Instant T0 = Instant.parse("2026-11-18T15:00:00Z");

    private static JwtService en(Instant momento) {
        return new JwtService(SECRETO, Clock.fixed(momento, ZoneOffset.UTC));
    }

    private static String b64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Un token válido devuelve el usuario, el inicio y el vencimiento")
    void valido() {
        JwtService jwt = en(T0);
        String token = jwt.emitir(12, T0.minus(Duration.ofHours(1)), T0.plus(Duration.ofMinutes(20)));
        JwtService.Token t = jwt.validar(token);
        assertNotNull(t);
        assertEquals(12, t.usuarioId());
        assertEquals(T0.minus(Duration.ofHours(1)), t.inicio());
        assertEquals(T0.plus(Duration.ofMinutes(20)), t.expira());
        assertEquals(T0, t.emitido());
    }

    @Test
    @DisplayName("Vence exactamente en su hora de expiración")
    void vence() {
        String token = en(T0).emitir(12, Duration.ofMinutes(20));
        assertNotNull(en(T0.plus(Duration.ofMinutes(20)).minusSeconds(1)).validar(token));
        assertNull(en(T0.plus(Duration.ofMinutes(20))).validar(token));
    }

    @Test
    @DisplayName("Se rechaza si cambian el contenido, la firma, el algoritmo o la clave")
    void alterado() {
        JwtService jwt = en(T0);
        String token = jwt.emitir(12, Duration.ofMinutes(20));
        String[] p = token.split("\\.");
        String otroCuerpo = b64("{\"sub\":\"1\",\"jti\":\"x\",\"iat\":1,\"exp\":9999999999,\"ini\":1}");
        assertNull(jwt.validar(p[0] + "." + otroCuerpo + "." + p[2]));
        assertNull(jwt.validar(p[0] + "." + p[1] + "." + p[2].substring(1)));
        assertNull(jwt.validar(b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + p[1] + "."));
        assertNull(new JwtService("otro-secreto-distinto-de-mas-de-32-caracteres", Clock.fixed(T0, ZoneOffset.UTC)).validar(token));
        assertNull(jwt.validar("no.es.token"));
        assertNull(jwt.validar(null));
    }

    @Test
    @DisplayName("Un token revocado deja de valer aunque no haya vencido")
    void revocado() {
        JwtService jwt = en(T0);
        String token = jwt.emitir(12, Duration.ofMinutes(20));
        String otro = jwt.emitir(12, Duration.ofMinutes(20));
        jwt.revocar(jwt.validar(token));
        assertNull(jwt.validar(token));
        assertNotNull(jwt.validar(otro));
    }

    @Test
    @DisplayName("La aplicación no arranca sin JWT_SECRET o con uno corto")
    void secretoObligatorio() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", Clock.systemUTC()));
        assertThrows(IllegalStateException.class, () -> new JwtService(null, Clock.systemUTC()));
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", Clock.systemUTC()));
    }
}

package com.vamospet.backend;

import com.vamospet.backend.seguridad.JwtService;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Duration;

public final class ConToken {

    public static final String SECRETO = "pruebas-vamospet-secreto-de-48-caracteres-0123456";
    public static final JwtService JWT = new JwtService(SECRETO, Clock.systemUTC());

    private ConToken() {
    }

    public static String token(Integer usuarioId) {
        return JWT.emitir(usuarioId, Duration.ofMinutes(20));
    }

    public static RequestPostProcessor como(Integer usuarioId) {
        return peticion -> {
            if (usuarioId != null) peticion.addHeader("Authorization", "Bearer " + token(usuarioId));
            return peticion;
        };
    }
}

package com.vamospet.backend.seguridad;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.permisos.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class SesionService {

    public static final Duration INACTIVIDAD_CLIENTE = Duration.ofMinutes(20);
    public static final Duration TOTAL_PERSONAL = Duration.ofHours(8);

    @Autowired
    private JwtService jwt;

    public record Sesion(String token, Instant expira) {
    }

    public static boolean esCliente(Usuario u) {
        return u != null && Rol.deId(u.getRol_id()) == Rol.CLIENTE;
    }

    public Sesion iniciar(Usuario u) {
        return emitir(u, jwt.ahora());
    }

    public Sesion renovar(Usuario u, JwtService.Token actual) {
        return emitir(u, actual.inicio());
    }

    private Sesion emitir(Usuario u, Instant inicio) {
        Instant expira = esCliente(u) ? jwt.ahora().plus(INACTIVIDAD_CLIENTE) : inicio.plus(TOTAL_PERSONAL);
        return new Sesion(jwt.emitir(u.getId(), inicio, expira), expira);
    }
}

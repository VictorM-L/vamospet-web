package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ClaveService {

    public static final int MAXIMO_BYTES = 72;

    @Autowired
    private PasswordEncoder encoder;

    public static boolean estaCifrada(String guardada) {
        return guardada != null && guardada.startsWith("$2");
    }

    public String codificar(String clave) {
        if (clave == null || clave.isBlank()) throw new NegocioException(400, "Escribe una contraseña.");
        if (clave.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw new NegocioException(400, "La contraseña es demasiado larga: usa menos letras con tilde o símbolos especiales.");
        }
        return encoder.encode(clave);
    }

    public boolean coincide(Usuario u, String claveIngresada) {
        String guardada = u.getContrasena();
        if (guardada == null || guardada.isBlank() || claveIngresada == null) return false;
        return estaCifrada(guardada) && encoder.matches(claveIngresada, guardada);
    }
}

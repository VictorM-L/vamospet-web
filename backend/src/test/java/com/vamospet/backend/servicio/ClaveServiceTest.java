package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClaveServiceTest {

    @Spy private PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    @InjectMocks private ClaveService claves;

    private Usuario con(String guardada) {
        Usuario u = new Usuario();
        u.setId(7);
        u.setContrasena(guardada);
        return u;
    }

    @Test
    @DisplayName("Toda contraseña nueva se guarda cifrada con BCrypt y se puede comprobar")
    void codificaSiempre() {
        String hash = claves.codificar("Clave2026");
        assertTrue(hash.startsWith("$2"));
        assertNotEquals(claves.codificar("Clave2026"), hash);
        assertTrue(claves.coincide(con(hash), "Clave2026"));
        assertFalse(claves.coincide(con(hash), "Clave2027"));
    }

    @Test
    @DisplayName("Una clave guardada en texto plano ya no permite entrar")
    void textoPlanoNoEntra() {
        Usuario u = con("Demo1234");
        assertFalse(claves.coincide(u, "Demo1234"));
        assertEquals("Demo1234", u.getContrasena());
    }

    @Test
    @DisplayName("Sin clave guardada o sin clave escrita no se entra")
    void sinClave() {
        assertFalse(claves.coincide(con(null), "Demo1234"));
        assertFalse(claves.coincide(con(""), "Demo1234"));
        assertFalse(claves.coincide(con("Demo1234"), null));
        assertEquals(400, assertThrows(NegocioException.class, () -> claves.codificar(" ")).getEstado());
    }

    @Test
    @DisplayName("Más de 72 bytes no se cifra: BCrypt cortaría la clave")
    void limiteBytes() {
        assertDoesNotThrow(() -> claves.codificar("a".repeat(63) + "1"));
        assertEquals(400, assertThrows(NegocioException.class, () -> claves.codificar("ñ".repeat(40) + "1")).getEstado());
    }
}

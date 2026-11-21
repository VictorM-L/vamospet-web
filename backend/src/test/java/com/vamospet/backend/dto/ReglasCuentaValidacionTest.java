package com.vamospet.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ReglasCuentaValidacionTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private RegistroRequest registro(String nombre, String dni, String correo, String telefono, String clave) {
        return new RegistroRequest(nombre, dni, correo, telefono, clave);
    }

    private Set<String> camposConError(Object dto) {
        Set<ConstraintViolation<Object>> errores = validador.validate(dto);
        return errores.stream().map(e -> e.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Un registro con datos correctos no tiene errores")
    void registroValido() {
        assertTrue(camposConError(registro("María José Ñahui", "12345678", "maria@correo.pe", "987654321", "Clave2026")).isEmpty());
    }

    @Test
    @DisplayName("Nombre con números o símbolos es rechazado")
    void nombreInvalido() {
        assertEquals(Set.of("nombre"), camposConError(registro("Juan 2", "12345678", "a@b.pe", "987654321", "Clave2026")));
        assertEquals(Set.of("nombre"), camposConError(registro("Dra. Ana", "12345678", "a@b.pe", "987654321", "Clave2026")));
        assertEquals(Set.of("nombre"), camposConError(registro("Ana  Ruiz", "12345678", "a@b.pe", "987654321", "Clave2026")));
    }

    @Test
    @DisplayName("Teléfono que no empieza en 9 o sin 9 dígitos es rechazado")
    void telefonoInvalido() {
        assertEquals(Set.of("telefono"), camposConError(registro("Ana Ruiz", "12345678", "a@b.pe", "887654321", "Clave2026")));
        assertEquals(Set.of("telefono"), camposConError(registro("Ana Ruiz", "12345678", "a@b.pe", "98765432", "Clave2026")));
        assertEquals(Set.of("telefono"), camposConError(registro("Ana Ruiz", "12345678", "a@b.pe", "98765432a", "Clave2026")));
    }

    @Test
    @DisplayName("DNI con letra y correo sin dominio son rechazados")
    void dniYCorreo() {
        assertEquals(Set.of("dni", "correo"), camposConError(registro("Ana Ruiz", "1234567A", "ana@correo", "987654321", "Clave2026")));
    }

    @Test
    @DisplayName("La contraseña pide entre 8 y 64 caracteres, al menos una letra y al menos un número")
    void contrasena() {
        for (String valida : new String[]{"Clave2026", "clave2026", "CLAVE2026", "Clave 2026", "Ñandú2026", "a1234567", "a".repeat(63) + "1"}) {
            assertTrue(camposConError(registro("Ana Ruiz", "12345678", "a@b.pe", "987654321", valida)).isEmpty(), valida);
        }
        for (String invalida : new String[]{"ClaveSinNum", "12345678", "Cl4ve", "a".repeat(64) + "1"}) {
            assertEquals(Set.of("contrasena"), camposConError(registro("Ana Ruiz", "12345678", "a@b.pe", "987654321", invalida)), invalida);
        }
    }

    @Test
    @DisplayName("En personal el teléfono es opcional, pero si se escribe debe ser válido")
    void telefonoOpcionalPersonal() {
        assertTrue(camposConError(new PersonalRequest("Lucía Prueba", "12345678", "l@v.pe", "", 3)).isEmpty());
        assertEquals(Set.of("telefono"), camposConError(new PersonalRequest("Lucía Prueba", "12345678", "l@v.pe", "12345", 3)));
    }

    @Test
    @DisplayName("El código de recuperación debe tener 6 dígitos")
    void codigoSeisDigitos() {
        assertTrue(camposConError(new CodigoRequest("a@b.pe", "012345")).isEmpty());
        assertEquals(Set.of("codigo"), camposConError(new CodigoRequest("a@b.pe", "12345")));
        assertEquals(Set.of("codigo"), camposConError(new CodigoRequest("a@b.pe", "12a456")));
    }
}

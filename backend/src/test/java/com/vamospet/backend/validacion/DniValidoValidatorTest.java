package com.vamospet.backend.validacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DniValidoValidatorTest {

    private final DniValidoValidator validador = new DniValidoValidator();

    @Test
    @DisplayName("Un DNI de 8 dígitos es válido")
    void ochoDigitos() {
        assertTrue(validador.isValid("12345678", null));
        assertTrue(DniValidoValidator.esValido("00000001"));
    }

    @ParameterizedTest(name = "\"{0}\" no es un DNI válido")
    @ValueSource(strings = { "1234567", "123456789", "1234567A", "ABCDEFGH", "1234 567", " 12345678", "", "１２３４５６７８" })
    @DisplayName("Se rechazan longitudes distintas, letras, espacios y dígitos no ASCII")
    void invalidos(String dni) {
        assertFalse(validador.isValid(dni, null));
    }

    @Test
    @DisplayName("El nulo lo resuelve @NotBlank, no este validador")
    void nuloLoDecideNotBlank() {
        assertTrue(validador.isValid(null, null));
        assertFalse(DniValidoValidator.esValido(null));
    }
}

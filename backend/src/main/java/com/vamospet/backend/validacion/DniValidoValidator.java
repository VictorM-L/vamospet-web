package com.vamospet.backend.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DniValidoValidator implements ConstraintValidator<DniValido, String> {

    public static boolean esValido(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || esValido(valor);
    }
}

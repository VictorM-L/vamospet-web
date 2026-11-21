package com.vamospet.backend.validacion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DniValidoValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface DniValido {
    String message() default "El DNI debe tener exactamente 8 dígitos, sin letras ni espacios.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

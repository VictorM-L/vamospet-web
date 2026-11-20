package com.vamospet.backend.dto;

import com.vamospet.backend.validacion.ReglasCuenta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CodigoRequest(
        @NotBlank(message = "Escribe tu correo.")
        @Email(regexp = ReglasCuenta.CORREO, message = ReglasCuenta.CORREO_MSG)
        String correo,

        @NotBlank(message = "Escribe el código que te enviamos.")
        @Pattern(regexp = ReglasCuenta.CODIGO, message = ReglasCuenta.CODIGO_MSG)
        String codigo) {
}

package com.vamospet.backend.dto;

import com.vamospet.backend.validacion.ReglasCuenta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorreoRequest(
        @NotBlank(message = "Escribe tu correo.")
        @Size(max = 100, message = "El correo no puede pasar de 100 caracteres.")
        @Email(regexp = ReglasCuenta.CORREO, message = ReglasCuenta.CORREO_MSG)
        String correo) {
}

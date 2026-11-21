package com.vamospet.backend.dto;

import com.vamospet.backend.validacion.ReglasCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        @Pattern(regexp = ReglasCuenta.NOMBRE, message = ReglasCuenta.NOMBRE_MSG)
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = ReglasCuenta.TELEFONO, message = ReglasCuenta.TELEFONO_MSG)
        String telefono,

        @Size(max = 255, message = "La dirección no puede pasar de 255 caracteres.")
        String direccion) {
}

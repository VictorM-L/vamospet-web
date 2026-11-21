package com.vamospet.backend.dto;

import com.vamospet.backend.validacion.ReglasCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactoRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        @Pattern(regexp = ReglasCuenta.NOMBRE, message = ReglasCuenta.NOMBRE_MSG)
        String nombre,

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 150, message = "El correo no puede pasar de 150 caracteres.")
        @Pattern(regexp = ReglasCuenta.CORREO, message = ReglasCuenta.CORREO_MSG)
        String correo,

        @Pattern(regexp = ReglasCuenta.TELEFONO_OPCIONAL, message = ReglasCuenta.TELEFONO_MSG)
        String telefono,

        @NotBlank(message = "Elige el motivo.")
        @Pattern(regexp = "^(Agendar una cita|Emergencia|Consulta sobre farmacia|Otro)$", message = "Elige un motivo de la lista.")
        String asunto,

        @NotBlank(message = "Escribe tu mensaje.")
        @Size(min = 10, max = 2000, message = "El mensaje debe tener entre 10 y 2000 caracteres.")
        String mensaje,

        String sitio_web) {
}

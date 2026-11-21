package com.vamospet.backend.dto;

import com.vamospet.backend.validacion.DniValido;
import com.vamospet.backend.validacion.ReglasCuenta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        @Pattern(regexp = ReglasCuenta.NOMBRE, message = ReglasCuenta.NOMBRE_MSG)
        String nombre,

        @NotBlank(message = "El DNI es obligatorio.")
        @DniValido
        String dni,

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 100, message = "El correo no puede pasar de 100 caracteres.")
        @Email(regexp = ReglasCuenta.CORREO, message = ReglasCuenta.CORREO_MSG)
        String correo,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = ReglasCuenta.TELEFONO, message = ReglasCuenta.TELEFONO_MSG)
        String telefono,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Pattern(regexp = ReglasCuenta.CONTRASENA, message = ReglasCuenta.CONTRASENA_MSG)
        String contrasena) {
}

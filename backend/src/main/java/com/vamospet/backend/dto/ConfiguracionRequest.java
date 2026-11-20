package com.vamospet.backend.dto;

import com.vamospet.backend.validacion.ReglasCuenta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfiguracionRequest(
        @NotBlank(message = "El nombre de la clínica es obligatorio.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        String nombre,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(min = 5, max = 200, message = "La dirección debe tener entre 5 y 200 caracteres.")
        String direccion,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = "^\\+?[0-9 ]{6,30}$", message = "El teléfono solo puede tener números, espacios y un + al inicio.")
        String telefono,

        @NotBlank(message = "El WhatsApp es obligatorio.")
        @Pattern(regexp = "^\\d{9,15}$", message = "El WhatsApp va solo con números y código de país, por ejemplo 51987654321.")
        String whatsapp,

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 150, message = "El correo no puede pasar de 150 caracteres.")
        @Pattern(regexp = ReglasCuenta.CORREO, message = ReglasCuenta.CORREO_MSG)
        String correo,

        @NotBlank(message = "El horario de atención es obligatorio.")
        @Size(min = 3, max = 150, message = "El horario debe tener entre 3 y 150 caracteres.")
        String horario_atencion,

        @NotNull(message = "Indica si atienden emergencias las 24 horas.")
        Boolean emergencias_24h,

        @NotNull(message = "Indica la hora de apertura del hospedaje.")
        @Min(value = 0, message = "La apertura del hospedaje va de 0 a 23.")
        @Max(value = 23, message = "La apertura del hospedaje va de 0 a 23.")
        Integer hospedaje_apertura,

        @NotNull(message = "Indica la hora de cierre del hospedaje.")
        @Min(value = 1, message = "El cierre del hospedaje va de 1 a 24.")
        @Max(value = 24, message = "El cierre del hospedaje va de 1 a 24.")
        Integer hospedaje_cierre,

        @NotNull(message = "Indica las horas mínimas para cancelar una cita.")
        @Min(value = 1, message = "Las horas para cancelar van de 1 a 168.")
        @Max(value = 168, message = "Las horas para cancelar van de 1 a 168.")
        Integer horas_cancelacion) {
}

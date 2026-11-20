package com.vamospet.backend.validacion;

public final class ReglasCuenta {

    public static final String NOMBRE = "^\\p{L}+(?: \\p{L}+)*$";
    public static final String NOMBRE_MSG = "El nombre solo puede tener letras y un espacio entre palabras.";

    public static final String TELEFONO = "^9\\d{8}$";
    public static final String TELEFONO_OPCIONAL = "^$|^9\\d{8}$";
    public static final String TELEFONO_MSG = "El teléfono debe tener 9 dígitos y empezar con 9.";

    public static final String CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    public static final String CORREO_MSG = "El correo no tiene un formato válido.";

    public static final String CONTRASENA = "(?s)^(?=.*\\p{L})(?=.*\\d).{8,64}$";
    public static final String CONTRASENA_MSG = "La contraseña debe tener entre 8 y 64 caracteres, al menos una letra y al menos un número.";

    public static final String CODIGO = "^\\d{6}$";
    public static final String CODIGO_MSG = "El código tiene 6 dígitos.";

    private ReglasCuenta() {
    }
}

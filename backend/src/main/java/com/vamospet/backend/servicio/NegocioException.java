package com.vamospet.backend.servicio;

public class NegocioException extends RuntimeException {
    private final int estado;

    public NegocioException(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int getEstado() { return estado; }
}

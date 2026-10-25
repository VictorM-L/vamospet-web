package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Usuario;

public interface CanalNotificacion {
    String nombre();
    void enviar(Usuario destinatario, String titulo, String mensaje) throws Exception;
}

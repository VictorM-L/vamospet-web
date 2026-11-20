package com.vamospet.backend.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.vamospet.backend.modelo.Usuario;

import java.time.Instant;

public class RespuestaSesion {

    @JsonUnwrapped
    private final Usuario usuario;
    private final String token;
    private final long token_expira;

    public RespuestaSesion(Usuario usuario, String token, Instant tokenExpira) {
        this.usuario = usuario;
        this.token = token;
        this.token_expira = tokenExpira.toEpochMilli();
    }

    public Usuario getUsuario() { return usuario; }
    public String getToken() { return token; }
    public long getToken_expira() { return token_expira; }
}

package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AutorizacionService {

    public static final int ADMIN = 1, CLIENTE = 2, VETERINARIO = 3, AUXILIAR = 4;

    @Autowired
    private UsuarioRepository usuarioRepositorio;

    public Usuario conRol(Integer usuarioId, int... rolesPermitidos) {
        if (usuarioId == null) return null;
        Usuario u = usuarioRepositorio.findById(usuarioId).orElse(null);
        if (u == null || Boolean.FALSE.equals(u.getActivo()) || u.getRol_id() == null) return null;
        final int rol = u.getRol_id();
        return Arrays.stream(rolesPermitidos).anyMatch(r -> r == rol) ? u : null;
    }
}

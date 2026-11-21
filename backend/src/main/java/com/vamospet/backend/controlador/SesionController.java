package com.vamospet.backend.controlador;

import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.seguridad.JwtService;
import com.vamospet.backend.seguridad.SesionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sesion")
@CrossOrigin(origins = "*")
public class SesionController {

    @Autowired
    private SesionService sesiones;

    @Autowired
    private JwtService jwt;

    @PostMapping("/renovar")
    public ResponseEntity<?> renovar() {
        JwtService.Token actual = UsuarioActual.token();
        SesionService.Sesion nueva = sesiones.renovar(UsuarioActual.obtener(), actual);
        jwt.revocar(actual);
        return ResponseEntity.ok(Map.of("token", nueva.token(), "token_expira", nueva.expira().toEpochMilli()));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrar() {
        jwt.revocar(UsuarioActual.token());
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada."));
    }
}

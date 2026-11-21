package com.vamospet.backend.controlador;

import com.vamospet.backend.dto.ConfiguracionRequest;
import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
public class ConfiguracionController {

    @Autowired
    private ConfiguracionService servicio;

    @GetMapping
    public ResponseEntity<?> obtener() {
        return ResponseEntity.ok(servicio.publica());
    }

    @PutMapping
    public ResponseEntity<?> guardar(@Valid @RequestBody ConfiguracionRequest datos) {
        return ResponseEntity.ok(servicio.guardar(UsuarioActual.id(), datos));
    }
}

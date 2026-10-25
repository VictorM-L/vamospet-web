package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.repositorio.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "*")
public class ServicioController {

    @Autowired
    private ServicioRepository repositorio;

    @GetMapping
    public List<Servicio> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return repositorio.findAll().stream()
                .filter(x -> incluirInactivos || !Boolean.FALSE.equals(x.getActivo()))
                .toList();
    }

    @PostMapping
    public Servicio guardar(@RequestBody Servicio servicio) { return repositorio.save(servicio); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Servicio existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        existente.setActivo(false);
        repositorio.save(existente);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Servicio existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        existente.setActivo(true);
        return ResponseEntity.ok(repositorio.save(existente));
    }
}

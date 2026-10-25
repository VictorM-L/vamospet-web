package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Especialidad;
import com.vamospet.backend.repositorio.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@CrossOrigin(origins = "*")
public class EspecialidadController {

    @Autowired
    private EspecialidadRepository repositorio;

    @GetMapping
    public List<Especialidad> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return repositorio.findAll().stream()
                .filter(x -> incluirInactivos || !Boolean.FALSE.equals(x.getActivo()))
                .toList();
    }

    @PostMapping
    public Especialidad guardar(@RequestBody Especialidad especialidad) { return repositorio.save(especialidad); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Especialidad existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        existente.setActivo(false);
        repositorio.save(existente);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Especialidad existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        existente.setActivo(true);
        return ResponseEntity.ok(repositorio.save(existente));
    }
}

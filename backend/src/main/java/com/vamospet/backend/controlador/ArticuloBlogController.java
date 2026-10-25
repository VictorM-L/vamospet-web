package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.ArticuloBlog;
import com.vamospet.backend.repositorio.ArticuloBlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/blog")
@CrossOrigin(origins = "*")
public class ArticuloBlogController {

    @Autowired
    private ArticuloBlogRepository repositorio;

    @GetMapping
    public List<ArticuloBlog> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return repositorio.findAll().stream()
                .filter(x -> incluirInactivos || !Boolean.FALSE.equals(x.getActivo()))
                .toList();
    }

    @PostMapping
    public ArticuloBlog guardar(@RequestBody ArticuloBlog articulo) { return repositorio.save(articulo); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        ArticuloBlog existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        existente.setActivo(false);
        repositorio.save(existente);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        ArticuloBlog existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        existente.setActivo(true);
        return ResponseEntity.ok(repositorio.save(existente));
    }
}

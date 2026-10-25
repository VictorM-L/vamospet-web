package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Categoria;
import com.vamospet.backend.repositorio.CategoriaRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {

    @Autowired
    private CategoriaRepository repositorio;

    @Autowired
    private ProductoRepository productoRepositorio;

    @GetMapping
    public List<Categoria> listar(@RequestParam(required = false) String tipo,
                                  @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<Categoria> todas = repositorio.findAll().stream()
                .filter(c -> incluirInactivos || !Boolean.FALSE.equals(c.getActivo()))
                .toList();
        if (tipo == null || tipo.isBlank()) return todas;
        return todas.stream().filter(c -> tipo.equals(c.getTipo())).toList();
    }

    @PostMapping
    public Categoria guardar(@RequestBody Categoria categoria) { return repositorio.save(categoria); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Categoria categoria = repositorio.findById(id).orElse(null);
        if (categoria == null) return ResponseEntity.notFound().build();

        boolean enUso = productoRepositorio.findAll().stream()
                .anyMatch(p -> id.equals(p.getCategoria_id()) && !Boolean.FALSE.equals(p.getActivo()));
        if (enUso) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "No se puede dar de baja: hay productos activos usando esta categoría."));
        }
        categoria.setActivo(false);
        repositorio.save(categoria);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Categoria categoria = repositorio.findById(id).orElse(null);
        if (categoria == null) return ResponseEntity.notFound().build();
        categoria.setActivo(true);
        return ResponseEntity.ok(repositorio.save(categoria));
    }
}

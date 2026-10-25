package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.ProductoImagen;
import com.vamospet.backend.repositorio.ProductoImagenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/producto-imagenes")
@CrossOrigin(origins = "*")
public class ProductoImagenController {

    @Autowired
    private ProductoImagenRepository repositorio;

    @GetMapping("/producto/{productoId}")
    public List<ProductoImagen> listarPorProducto(@PathVariable Integer productoId) {
        return repositorio.findAll().stream()
                .filter(i -> productoId.equals(i.getProducto_id()) && !Boolean.FALSE.equals(i.getActivo()))
                .toList();
    }

    @PostMapping
    public ProductoImagen guardar(@RequestBody ProductoImagen imagen) { return repositorio.save(imagen); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        ProductoImagen imagen = repositorio.findById(id).orElse(null);
        if (imagen == null) return ResponseEntity.notFound().build();
        imagen.setActivo(false);
        repositorio.save(imagen);
        return ResponseEntity.ok().build();
    }
}

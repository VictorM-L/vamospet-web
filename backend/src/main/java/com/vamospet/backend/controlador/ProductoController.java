package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.repositorio.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoRepository repositorio;

    @GetMapping
    public List<Producto> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return repositorio.findAll().stream()
                .filter(p -> incluirInactivos || !Boolean.FALSE.equals(p.getActivo()))
                .toList();
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Integer id) { return repositorio.findById(id).orElse(null); }

    @GetMapping("/alertas")
    public Map<String, Object> alertas() {
        List<Producto> todos = repositorio.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .toList();
        LocalDate limiteVencimiento = LocalDate.now().plusDays(30);

        List<Producto> stockBajo = todos.stream()
                .filter(p -> !"Descontinuado".equals(p.getEstado()))
                .filter(p -> p.getStock() != null && p.getStock_minimo() != null && p.getStock() <= p.getStock_minimo())
                .toList();

        List<Producto> porVencer = todos.stream()
                .filter(p -> !"Descontinuado".equals(p.getEstado()))
                .filter(p -> p.getFecha_vencimiento() != null && !p.getFecha_vencimiento().isBefore(LocalDate.now()) && !p.getFecha_vencimiento().isAfter(limiteVencimiento))
                .toList();

        return Map.of("stockBajo", stockBajo, "porVencer", porVencer);
    }

    @PostMapping
    public Producto guardar(@RequestBody Producto producto) { return repositorio.save(producto); }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Producto datos) {
        Producto existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();

        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setSku(datos.getSku());
        existente.setPrecio(datos.getPrecio());
        existente.setPrecio_costo(datos.getPrecio_costo());
        existente.setStock_minimo(datos.getStock_minimo());
        existente.setRequiere_receta(datos.getRequiere_receta());
        existente.setTipo(datos.getTipo());
        existente.setMarca(datos.getMarca());
        if (datos.getImagen() != null) existente.setImagen(datos.getImagen());
        existente.setUnidad_presentacion(datos.getUnidad_presentacion());
        existente.setProveedor(datos.getProveedor());
        existente.setFecha_vencimiento(datos.getFecha_vencimiento());
        existente.setCategoria_id(datos.getCategoria_id());
        if (datos.getEstado() != null) existente.setEstado(datos.getEstado());

        return ResponseEntity.ok(repositorio.save(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Producto producto = repositorio.findById(id).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        producto.setActivo(false);
        producto.setEstado("Descontinuado");
        repositorio.save(producto);
        return ResponseEntity.ok(Map.of("mensaje", "Producto dado de baja. Su historial se conserva y puedes reactivarlo cuando quieras."));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Producto producto = repositorio.findById(id).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        producto.setActivo(true);
        producto.setEstado(producto.getStock() != null && producto.getStock() > 0 ? "Activo" : "Agotado");
        return ResponseEntity.ok(repositorio.save(producto));
    }
}

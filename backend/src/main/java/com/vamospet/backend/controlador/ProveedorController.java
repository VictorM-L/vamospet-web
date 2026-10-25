package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Proveedor;
import com.vamospet.backend.repositorio.ProveedorRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "*")
public class ProveedorController {

    @Autowired
    private ProveedorRepository repositorio;

    @GetMapping
    public List<Proveedor> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return repositorio.findAll().stream()
                .filter(p -> incluirInactivos || !Boolean.FALSE.equals(p.getActivo()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Proveedor p) {
        String error = validar(p, null);
        if (error != null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", error));
        p.setId(null);
        p.setActivo(true);
        return ResponseEntity.ok(repositorio.save(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Proveedor datos) {
        Proveedor p = repositorio.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        String error = validar(datos, id);
        if (error != null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", error));
        p.setRazon_social(datos.getRazon_social());
        p.setRuc(datos.getRuc());
        p.setContacto(datos.getContacto());
        p.setTelefono(datos.getTelefono());
        p.setCorreo(datos.getCorreo());
        p.setDireccion(datos.getDireccion());
        return ResponseEntity.ok(repositorio.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id) {
        Proveedor p = repositorio.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setActivo(false);
        return ResponseEntity.ok(repositorio.save(p));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Proveedor p = repositorio.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setActivo(true);
        return ResponseEntity.ok(repositorio.save(p));
    }

    private String validar(Proveedor p, Integer idExcluido) {
        if (StringUtils.isBlank(p.getRazon_social())) return "La razón social es obligatoria.";
        p.setRazon_social(StringUtils.normalizeSpace(p.getRazon_social()));
        String ruc = StringUtils.deleteWhitespace(StringUtils.defaultString(p.getRuc()));
        if (StringUtils.isBlank(ruc)) return "El RUC es obligatorio.";
        if (!StringUtils.isNumeric(ruc)) return "El RUC solo debe contener números.";
        if (ruc.length() != 11) return "El RUC debe tener 11 dígitos.";
        if (!StringUtils.startsWithAny(ruc, "10", "15", "17", "20"))
            return "El RUC debe empezar con 10, 15, 17 o 20.";
        p.setRuc(ruc);
        boolean repetido = repositorio.findAll().stream().anyMatch(o ->
                ruc.equals(o.getRuc()) && (idExcluido == null || !idExcluido.equals(o.getId())));
        if (repetido) return "Ya existe un proveedor con ese RUC.";
        return null;
    }
}

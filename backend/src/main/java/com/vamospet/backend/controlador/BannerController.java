package com.vamospet.backend.controlador;

import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.BannerService;
import com.vamospet.backend.servicio.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/banners")
@CrossOrigin(origins = "*")
public class BannerController {

    @Autowired
    private BannerService servicio;

    @GetMapping
    public ResponseEntity<?> activos(@RequestParam String seccion) {
        return ResponseEntity.ok(servicio.activos(seccion));
    }

    @GetMapping("/gestion")
    public ResponseEntity<?> gestion(@RequestParam String seccion) {
        return ResponseEntity.ok(servicio.gestion(UsuarioActual.id(), seccion));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestParam("seccion") String seccion,
                                   @RequestParam(value = "imagen_pc", required = false) MultipartFile pc,
                                   @RequestParam(value = "imagen_movil", required = false) MultipartFile movil,
                                   @RequestParam(value = "activo", required = false) Boolean activo) {
        return ResponseEntity.ok(servicio.crear(UsuarioActual.id(), seccion, archivo(pc), archivo(movil), activo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> reemplazar(@PathVariable Integer id,
                                        @RequestParam(value = "imagen_pc", required = false) MultipartFile pc,
                                        @RequestParam(value = "imagen_movil", required = false) MultipartFile movil,
                                        @RequestParam(value = "quitar_movil", required = false, defaultValue = "false") boolean quitarMovil) {
        return ResponseEntity.ok(servicio.reemplazar(UsuarioActual.id(), id, archivo(pc), archivo(movil), quitarMovil));
    }

    @PatchMapping("/{id}/orden")
    public ResponseEntity<?> orden(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(servicio.mover(UsuarioActual.id(), id, entero(b.get("posicion"))));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<?> activo(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        Object v = b.get("activo");
        return ResponseEntity.ok(servicio.cambiarActivo(UsuarioActual.id(), id, v == null ? null : Boolean.valueOf(v.toString())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        servicio.eliminar(UsuarioActual.id(), id);
        return ResponseEntity.ok(Map.of("mensaje", "Banner eliminado."));
    }

    private static BannerService.Archivo archivo(MultipartFile f) {
        if (f == null || f.isEmpty()) return null;
        try {
            return new BannerService.Archivo(f.getOriginalFilename(), f.getBytes());
        } catch (IOException e) {
            throw new NegocioException(400, "No se pudo leer la imagen enviada.");
        }
    }

    private static Integer entero(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try { return Integer.valueOf(v.toString().trim()); } catch (NumberFormatException e) { throw new NegocioException(400, "La posición no es válida."); }
    }
}

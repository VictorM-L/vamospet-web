package com.vamospet.backend.controlador;

import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.CirugiaService;
import com.vamospet.backend.servicio.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/cirugias")
@CrossOrigin(origins = "*")
public class CirugiaController {

    @Autowired
    private CirugiaService servicio;

    @GetMapping("/tablero")
    public ResponseEntity<?> tablero() {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(servicio.tablero(usuarioId != null ? usuarioId : UsuarioActual.id()));
    }

    @PatchMapping("/{id}/siguiente")
    public ResponseEntity<?> siguiente(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        CirugiaService.Paso paso = new CirugiaService.Paso(texto(b, "resultado"), texto(b, "complicaciones"), texto(b, "medicamentos"),
                texto(b, "decision"), fechaHora(b, "ingreso"), fechaHora(b, "salida_estimada"), texto(b, "observaciones"));
        return ResponseEntity.ok(servicio.siguiente(entero(b, "usuario_id"), id, paso));
    }

    @PatchMapping("/{id}/notas")
    public ResponseEntity<?> notas(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(servicio.guardarNotas(entero(b, "usuario_id"), id, texto(b, "notas")));
    }

    @GetMapping("/{id}/linea-tiempo")
    public ResponseEntity<?> lineaTiempo(@PathVariable Integer id) {
        return ResponseEntity.ok(servicio.lineaTiempo(id));
    }

    private static Integer entero(Map<String, Object> b, String k) {
        try {
            return b.get(k) == null || b.get(k).toString().isBlank() ? null : Integer.valueOf(b.get(k).toString());
        } catch (NumberFormatException e) {
            throw new NegocioException(400, "El campo " + k + " no es válido.");
        }
    }

    private static String texto(Map<String, Object> b, String k) {
        return b.get(k) == null ? null : b.get(k).toString();
    }

    private static LocalDateTime fechaHora(Map<String, Object> b, String k) {
        String t = texto(b, k);
        if (t == null || t.isBlank()) return null;
        try {
            return LocalDateTime.parse(t.length() == 16 ? t + ":00" : t);
        } catch (DateTimeParseException e) {
            throw new NegocioException(400, "La fecha " + k + " no es válida.");
        }
    }
}

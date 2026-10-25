package com.vamospet.backend.controlador;

import com.vamospet.backend.servicio.AtencionMedicaService;
import com.vamospet.backend.servicio.AtencionMedicaService.AtencionException;
import com.vamospet.backend.servicio.AtencionMedicaService.DatosAtencion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
public class AtencionMedicaController {

    @Autowired
    private AtencionMedicaService servicio;

    @PostMapping("/{id}/atender")
    public ResponseEntity<?> atender(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Integer usuarioId = body.get("usuario_id") == null ? null : Integer.valueOf(body.get("usuario_id").toString());
            LocalDate proxima = texto(body, "vacuna_proxima_dosis") == null ? null : LocalDate.parse(texto(body, "vacuna_proxima_dosis"));
            DatosAtencion datos = new DatosAtencion(texto(body, "diagnostico"), texto(body, "tratamiento"),
                    texto(body, "vacuna_nombre"), proxima);
            return ResponseEntity.ok(servicio.atender(id, usuarioId, datos));
        } catch (AtencionException e) {
            return ResponseEntity.status(e.getEstado()).body(Map.of("error", e.getMessage()));
        } catch (NumberFormatException | DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos (revisa las fechas y el usuario)."));
        }
    }

    private static String texto(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v == null ? null : v.toString();
    }
}

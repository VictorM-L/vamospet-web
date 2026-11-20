package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.servicio.AutorizacionService;
import com.vamospet.backend.servicio.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
public class CobroCitaController {

    @Autowired private PagoService pagoService;
    @Autowired private AutorizacionService autorizacion;

    @PostMapping("/{id}/cobrar")
    public ResponseEntity<?> cobrar(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        Integer usuarioId = entero(b.get("usuario_id"));
        Usuario cobrador = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR);
        if (cobrador == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo el Administrador o el Auxiliar pueden cobrar citas."));
        }
        PagoService.Resultado r = pagoService.cobrar(cobrador.getId(), "cita", id, PagoController.datos(b));
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pago", r.pago());
        cuerpo.put("comprobante", r.comprobante());
        return ResponseEntity.ok(cuerpo);
    }

    private static Integer entero(Object o) {
        try {
            return o == null || o.toString().isBlank() ? null : Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

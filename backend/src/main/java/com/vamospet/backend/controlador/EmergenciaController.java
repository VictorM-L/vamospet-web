package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.ContactoEmergencia;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.servicio.EmergenciaService;
import com.vamospet.backend.servicio.EmergenciaService.DatosEmergencia;
import com.vamospet.backend.servicio.NegocioException;
import com.vamospet.backend.servicio.TicketEmergenciaPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class EmergenciaController {

    @Autowired private EmergenciaService servicio;
    @Autowired private TicketEmergenciaPdfService ticketPdf;

    @PostMapping("/auxiliar/emergencia/registrar")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> b) {
        Mascota m = servicio.registrar(entero(b.get("usuario_id")), new DatosEmergencia(txt(b, "nombre_acompanante"), txt(b, "telefono"),
                txt(b, "mascota_nombre"), txt(b, "especie"), txt(b, "raza"), txt(b, "sexo"), txt(b, "motivo")));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mascota_id", m.getId());
        r.put("codigo", m.getCodigo_unico_temporal());
        return ResponseEntity.ok(r);
    }

    @GetMapping("/auxiliar/emergencia/pendientes")
    public ResponseEntity<?> pendientes(@RequestParam Integer usuarioId) {
        return ResponseEntity.ok(servicio.sinVincular(usuarioId));
    }

    @GetMapping("/auxiliar/emergencia/ticket/{idMascota}")
    public ResponseEntity<byte[]> ticket(@PathVariable Integer idMascota, @RequestParam Integer usuarioId) throws Exception {
        Map<String, Object> d = servicio.datosTicket(usuarioId, idMascota);
        byte[] pdf = ticketPdf.generar((String) d.get("codigo"), (Mascota) d.get("mascota"), (ContactoEmergencia) d.get("contacto"));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ticket-" + d.get("codigo") + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/cliente/vincular-mascota")
    public ResponseEntity<?> vincular(@RequestBody Map<String, Object> b) {
        Mascota m = servicio.vincular(entero(b.get("usuario_id")), txt(b, "codigo"));
        return ResponseEntity.ok(Map.of("mascota_id", m.getId(), "nombre", m.getNombre(),
                "mensaje", m.getNombre() + " ya está en tu cuenta con todo su historial."));
    }

    private static Integer entero(Object o) {
        try {
            return o == null || o.toString().isBlank() ? null : Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            throw new NegocioException(400, "Usuario inválido.");
        }
    }

    private static String txt(Map<String, Object> b, String k) { return b.get(k) == null ? null : b.get(k).toString(); }
}

package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.ArchivoExamen;
import com.vamospet.backend.servicio.ExamenService;
import com.vamospet.backend.servicio.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExamenController {

    @Autowired
    private ExamenService servicio;

    @PostMapping("/veterinario/examenes/solicitar")
    public ResponseEntity<?> solicitar(@RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(servicio.solicitar(entero(b, "usuario_id"), entero(b, "mascota_id"), entero(b, "cita_id"),
                texto(b, "tipo_examen"), texto(b, "cirugia_prevista"), texto(b, "indicaciones")));
    }

    @GetMapping("/veterinario/examenes/pendientes")
    public ResponseEntity<?> pendientes(@RequestParam Integer usuarioId) {
        return ResponseEntity.ok(servicio.listar(usuarioId, null, Set.of("Resultados_Subidos")));
    }

    @PutMapping("/veterinario/examenes/evaluar/{idOrden}")
    public ResponseEntity<?> evaluar(@PathVariable Integer idOrden, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(servicio.evaluar(entero(b, "usuario_id"), idOrden, texto(b, "decision"), texto(b, "observaciones")));
    }

    @PostMapping("/veterinario/cirugias/agendar")
    public ResponseEntity<?> agendarCirugia(@RequestBody Map<String, Object> b) {
        try {
            String f = texto(b, "fecha_hora");
            return ResponseEntity.ok(servicio.agendarCirugia(entero(b, "usuario_id"), entero(b, "orden_id"),
                    f == null ? null : LocalDateTime.parse(f), texto(b, "tipo_cirugia")));
        } catch (DateTimeParseException e) {
            throw new NegocioException(400, "Fecha y hora inválidas.");
        }
    }

    @PatchMapping("/veterinario/cirugias/{id}/estado")
    public ResponseEntity<?> estadoCirugia(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(servicio.cambiarEstadoCirugia(entero(b, "usuario_id"), id, texto(b, "estado"), texto(b, "resultado"), texto(b, "medicamentos")));
    }

    @PostMapping("/auxiliar/examenes/subir")
    public ResponseEntity<?> subirPersonal(@RequestParam("usuario_id") Integer usuarioId, @RequestParam("orden_id") Integer ordenId,
                                           @RequestParam("archivo") MultipartFile archivo) {
        return subir(usuarioId, ordenId, archivo, true);
    }

    @PostMapping("/cliente/examenes/subir-pdf")
    public ResponseEntity<?> subirCliente(@RequestParam("usuario_id") Integer usuarioId, @RequestParam("orden_id") Integer ordenId,
                                          @RequestParam("archivo") MultipartFile archivo) {
        return subir(usuarioId, ordenId, archivo, false);
    }

    private ResponseEntity<?> subir(Integer usuarioId, Integer ordenId, MultipartFile archivo, boolean personal) {
        try {
            ArchivoExamen a = servicio.subirArchivo(usuarioId, ordenId, archivo.getOriginalFilename(), archivo.getBytes());
            return ResponseEntity.ok(Map.of("id", a.getId(), "nombre", a.getNombre_original(), "mensaje", "Resultado subido. El veterinario lo revisará."));
        } catch (IOException e) {
            throw new NegocioException(400, "No se pudo leer el archivo enviado.");
        }
    }

    @GetMapping("/examenes")
    public ResponseEntity<?> listar(@RequestParam Integer usuarioId, @RequestParam(required = false) Integer mascotaId,
                                    @RequestParam(required = false) String estados) {
        Set<String> filtro = estados == null || estados.isBlank() ? null
                : Arrays.stream(estados.split(",")).map(String::trim).collect(Collectors.toSet());
        return ResponseEntity.ok(servicio.listar(usuarioId, mascotaId, filtro));
    }

    @GetMapping("/cirugias")
    public ResponseEntity<?> cirugias(@RequestParam Integer usuarioId, @RequestParam(required = false) Integer mascotaId) {
        return ResponseEntity.ok(servicio.cirugias(usuarioId, mascotaId));
    }

    @GetMapping("/examenes/archivo/{id}")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer id, @RequestParam Integer usuarioId) {
        ExamenService.Descarga d = servicio.descargar(usuarioId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.archivo().getTipo_contenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resultado-" + d.archivo().getId()
                        + (d.archivo().getTipo_contenido().contains("pdf") ? ".pdf" : ".jpg") + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(d.contenido());
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
}

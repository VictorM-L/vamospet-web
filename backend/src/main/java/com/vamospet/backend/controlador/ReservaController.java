package com.vamospet.backend.controlador;

import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReservaController {

    @Autowired private EstanciaService estanciaService;
    @Autowired private ViajeService viajeService;

    @GetMapping("/hospedaje/configuracion")
    public ResponseEntity<?> configuracion() {
        return ResponseEntity.ok(estanciaService.configuracion());
    }

    @GetMapping("/hospedaje/disponibilidad")
    public ResponseEntity<?> disponibilidad(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ingreso,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salida,
                                            @RequestParam(required = false) String modalidad) {
        return ResponseEntity.ok(estanciaService.disponibilidad(ingreso, salida, modalidad));
    }

    @PostMapping("/cliente/hospedaje/reservar")
    public ResponseEntity<?> reservar(@RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(estanciaService.reservar(entero(b, "usuario_id"), texto(b, "modalidad"), entero(b, "mascota_id"),
                fechaHora(b, "ingreso"), fechaHora(b, "salida"), texto(b, "cuidados_especiales"), PagoController.datos(b)));
    }

    @GetMapping("/estancias")
    public ResponseEntity<?> estancias() {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(estanciaService.listar(usuarioId));
    }

    @GetMapping("/estancias/cuartos")
    public ResponseEntity<?> cuartos() {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(estanciaService.estadoCuartos(usuarioId));
    }

    @PostMapping("/veterinario/internamientos")
    public ResponseEntity<?> internar(@RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(estanciaService.internar(entero(b, "usuario_id"), entero(b, "cirugia_id"),
                fechaHora(b, "ingreso"), fechaHora(b, "salida_estimada"), texto(b, "observaciones")));
    }

    @PatchMapping("/estancias/{id}/ingreso")
    public ResponseEntity<?> ingreso(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(estanciaService.registrarIngreso(entero(b, "usuario_id"), id));
    }

    @PatchMapping("/estancias/{id}/salida")
    public ResponseEntity<?> salida(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(estanciaService.finalizar(entero(b, "usuario_id"), id));
    }

    @PatchMapping("/estancias/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(estanciaService.cancelar(entero(b, "usuario_id"), id));
    }

    @GetMapping("/viajes/tarifa")
    public ResponseEntity<?> tarifa() {
        return ResponseEntity.ok(viajeService.tarifa());
    }

    @GetMapping("/viajes/requisitos")
    public ResponseEntity<?> requisitos() {
        return ResponseEntity.ok(viajeService.requisitos());
    }

    @PostMapping("/cliente/viajes/solicitar")
    public ResponseEntity<?> solicitarViaje(@RequestParam("mascotas") String mascotas,
                                            @RequestParam("fecha_viaje") String fechaViaje,
                                            @RequestParam("destino") String destino,
                                            @RequestParam("metodo") String metodo,
                                            @RequestParam(value = "correo_tarjeta", required = false) String correo,
                                            @RequestParam(value = "tarjeta_ultimos4", required = false) String ultimos4,
                                            @RequestParam(value = "operacion_yape", required = false) String operacion,
                                            @RequestParam(value = "certificado_salud", required = false) MultipartFile certificadoSalud,
                                            @RequestParam(value = "microchip", required = false) MultipartFile microchip,
                                            @RequestParam(value = "vacunas", required = false) MultipartFile vacunas) {
        Integer usuarioId = UsuarioActual.id();
        List<Integer> ids = new ArrayList<>();
        for (String p : mascotas.split(",")) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try { ids.add(Integer.valueOf(t)); } catch (NumberFormatException e) { throw new NegocioException(400, "Las mascotas elegidas no son válidas."); }
        }
        LocalDate fecha;
        try { fecha = LocalDate.parse(fechaViaje); } catch (RuntimeException e) { throw new NegocioException(400, "La fecha de viaje no es válida."); }
        return ResponseEntity.ok(viajeService.solicitar(usuarioId, ids, fecha, destino, archivos(certificadoSalud, microchip, vacunas),
                new PagoService.DatosPago(metodo, correo, ultimos4, operacion)));
    }

    @PostMapping("/cliente/viajes/{id}/documentos")
    public ResponseEntity<?> reenviarDocumentos(@PathVariable Integer id,
                                                @RequestParam(value = "certificado_salud", required = false) MultipartFile certificadoSalud,
                                                @RequestParam(value = "microchip", required = false) MultipartFile microchip,
                                                @RequestParam(value = "vacunas", required = false) MultipartFile vacunas) {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(viajeService.reenviarDocumentos(usuarioId, id, archivos(certificadoSalud, microchip, vacunas)));
    }

    @GetMapping("/viajes/{id}/documentos/{docId}")
    public ResponseEntity<byte[]> documentoRequisito(@PathVariable Integer id, @PathVariable Integer docId) {
        Integer usuarioId = UsuarioActual.id();
        return archivo(viajeService.documentoRequisito(usuarioId, id, docId));
    }

    private static Map<String, ViajeService.Archivo> archivos(MultipartFile certificadoSalud, MultipartFile microchip, MultipartFile vacunas) {
        Map<String, ViajeService.Archivo> m = new LinkedHashMap<>();
        agregar(m, "certificado_salud", certificadoSalud);
        agregar(m, "microchip", microchip);
        agregar(m, "vacunas", vacunas);
        return m;
    }

    private static void agregar(Map<String, ViajeService.Archivo> m, String clave, MultipartFile f) {
        if (f == null || f.isEmpty()) return;
        try {
            m.put(clave, new ViajeService.Archivo(f.getOriginalFilename(), f.getBytes()));
        } catch (IOException e) {
            throw new NegocioException(400, "No se pudo leer el documento enviado.");
        }
    }

    private static ResponseEntity<byte[]> archivo(Object[] d) {
        String nombre = URLEncoder.encode((String) d[2], StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + nombre)
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType((String) d[1]))
                .body((byte[]) d[0]);
    }

    @GetMapping("/viajes")
    public ResponseEntity<?> viajes() {
        Integer usuarioId = UsuarioActual.id();
        return ResponseEntity.ok(viajeService.listar(usuarioId));
    }

    @PatchMapping("/viajes/{id}/revisar")
    public ResponseEntity<?> revisar(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        List<String> observados = new ArrayList<>();
        Object o = b.get("observados");
        if (o instanceof List<?> lista) lista.forEach(x -> { if (x != null) observados.add(x.toString()); });
        else if (o != null) for (String t : o.toString().split(",")) observados.add(t.trim());
        return ResponseEntity.ok(viajeService.revisar(entero(b, "usuario_id"), id, texto(b, "decision"), texto(b, "observaciones"), observados));
    }

    @GetMapping("/viajes/{id}/documento")
    public ResponseEntity<byte[]> documento(@PathVariable Integer id) {
        Integer usuarioId = UsuarioActual.id();
        return archivo(viajeService.documento(usuarioId, id));
    }

    private static Integer entero(Map<String, Object> b, String k) {
        Object v = b.get(k);
        if (v == null || v.toString().isBlank()) return null;
        try { return Integer.valueOf(v.toString().trim()); } catch (NumberFormatException e) { throw new NegocioException(400, "El campo " + k + " no es válido."); }
    }

    private static String texto(Map<String, Object> b, String k) {
        Object v = b.get(k);
        return v == null ? null : v.toString();
    }

    private static LocalDateTime fechaHora(Map<String, Object> b, String k) {
        String t = texto(b, k);
        if (t == null || t.isBlank()) return null;
        try { return LocalDateTime.parse(t.trim()); } catch (RuntimeException e) { throw new NegocioException(400, "La fecha del campo " + k + " no es válida."); }
    }
}

package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.ReporteExcelService;
import com.vamospet.backend.servicio.ReportePdfService;
import com.vamospet.backend.servicio.ReporteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes/formal")
@CrossOrigin(origins = "*")
public class ReporteFormalController {

    private static final Logger log = LoggerFactory.getLogger(ReporteFormalController.class);
    private enum Formato { JSON, PDF, XLSX }

    @Autowired private ReporteService reporteService;
    @Autowired private ReportePdfService pdfService;
    @Autowired private ReporteExcelService excelService;
    @Autowired private UsuarioRepository usuarioRepositorio;

    @GetMapping("/{tipo}")
    public ResponseEntity<?> ver(@PathVariable String tipo, @RequestParam Integer usuarioId,
                                 @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta) {
        return preparar(tipo, usuarioId, desde, hasta, Formato.JSON);
    }

    @GetMapping("/{tipo}/pdf")
    public ResponseEntity<?> pdf(@PathVariable String tipo, @RequestParam Integer usuarioId,
                                 @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta) {
        return preparar(tipo, usuarioId, desde, hasta, Formato.PDF);
    }

    @GetMapping("/{tipo}/xlsx")
    public ResponseEntity<?> xlsx(@PathVariable String tipo, @RequestParam Integer usuarioId,
                                  @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta) {
        return preparar(tipo, usuarioId, desde, hasta, Formato.XLSX);
    }

    private ResponseEntity<?> preparar(String tipo, Integer usuarioId, String desde, String hasta, Formato formato) {
        Usuario u = usuarioRepositorio.findById(usuarioId).orElse(null);
        if (u == null || !Integer.valueOf(1).equals(u.getRol_id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Los reportes son solo para el Administrador."));
        }
        if (!ReporteService.TIPOS.contains(tipo)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Reporte desconocido."));
        }
        LocalDate d, h;
        try {
            h = hasta == null || hasta.isBlank() ? LocalDate.now() : LocalDate.parse(hasta);
            d = desde == null || desde.isBlank() ? h.minusDays(30) : LocalDate.parse(desde);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fechas inválidas."));
        }
        if (d.isAfter(h)) return ResponseEntity.badRequest().body(Map.of("error", "La fecha 'desde' no puede ser posterior a 'hasta'."));

        ReporteService.Tabla tabla = reporteService.generar(tipo, d, h);
        try {
            return switch (formato) {
                case JSON -> ResponseEntity.ok(tabla);
                case PDF -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-" + tipo + ".pdf\"")
                        .body(pdfService.generar(tabla));
                case XLSX -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(ReporteExcelService.CONTENT_TYPE))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-" + tipo + ".xlsx\"")
                        .body(excelService.generar(tabla));
            };
        } catch (Exception e) {
            log.error("No se pudo generar el reporte {} en formato {}", tipo, formato, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo generar el archivo."));
        }
    }
}

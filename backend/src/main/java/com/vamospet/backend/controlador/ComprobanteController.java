package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Comprobante;
import com.vamospet.backend.modelo.Pago;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.ComprobanteRepository;
import com.vamospet.backend.repositorio.PagoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.servicio.ComprobantePdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/comprobantes")
@CrossOrigin(origins = "*")
public class ComprobanteController {

    @Autowired private ComprobanteRepository repositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private ComprobantePdfService pdfService;

    @GetMapping
    public List<Comprobante> listar() { return repositorio.findAll(); }

    @PostMapping
    public Comprobante guardar(@RequestBody Comprobante comprobante) {
        comprobante.setId(null);
        comprobante.setCodigo_verificacion(UUID.randomUUID().toString());
        return repositorio.save(comprobante);
    }

    @GetMapping("/por-pedido/{pedidoId}")
    public ResponseEntity<?> porPedido(@PathVariable Integer pedidoId) {
        for (Pago p : pagoRepositorio.findAll()) {
            if (!pedidoId.equals(p.getPedido_id())) continue;
            for (Comprobante c : repositorio.findAll()) {
                if (p.getId().equals(c.getPago_id())) return ResponseEntity.ok(c);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> pdf(@PathVariable Integer id, @RequestParam Integer usuarioId) {
        Comprobante c = repositorio.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        Usuario u = usuarioRepositorio.findById(usuarioId).orElse(null);
        Pago pago = c.getPago_id() == null ? null : pagoRepositorio.findById(c.getPago_id()).orElse(null);
        boolean personal = u != null && (Integer.valueOf(1).equals(u.getRol_id()) || Integer.valueOf(4).equals(u.getRol_id()));
        boolean dueno = u != null && pago != null && u.getId().equals(pago.getUsuario_id());
        if (!personal && !dueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No tienes acceso a este comprobante."));
        }
        try {
            asegurarCodigo(c);
            byte[] bytes = pdfService.generar(c);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + c.getNumero() + ".pdf\"")
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo generar el PDF."));
        }
    }

    @GetMapping("/verificar/{codigo}")
    public ResponseEntity<?> verificar(@PathVariable String codigo) {
        Comprobante c = repositorio.findAll().stream()
                .filter(x -> codigo.equals(x.getCodigo_verificacion())).findFirst().orElse(null);
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("valido", false));
        Pago pago = c.getPago_id() == null ? null : pagoRepositorio.findById(c.getPago_id()).orElse(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("valido", true);
        r.put("tipo", c.getTipo());
        r.put("numero", c.getNumero());
        r.put("monto", pago != null ? pago.getMonto() : null);
        r.put("fecha", pago != null && pago.getFecha() != null ? pago.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null);
        r.put("emisor", "Vamos Pet Clínica Veterinaria");
        return ResponseEntity.ok(r);
    }

    private void asegurarCodigo(Comprobante c) {
        if (c.getCodigo_verificacion() == null || c.getCodigo_verificacion().isBlank()) {
            c.setCodigo_verificacion(UUID.randomUUID().toString());
            repositorio.save(c);
        }
    }
}

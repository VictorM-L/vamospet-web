package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.CitaCostoAdicional;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/citas/{citaId}/costos-adicionales")
@CrossOrigin(origins = "*")
public class CostoAdicionalController {

    @Autowired private CitaCostoAdicionalRepository repositorio;
    @Autowired private CitaRepository citaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;

    @GetMapping
    public ResponseEntity<?> listar(@PathVariable Integer citaId) {
        Cita cita = citaRepositorio.findById(citaId).orElse(null);
        if (cita == null) return ResponseEntity.notFound().build();
        List<CitaCostoAdicional> extras = repositorio.findAll().stream()
                .filter(c -> citaId.equals(c.getCita_id())).toList();
        BigDecimal base = cita.getCosto();
        if (base == null && cita.getServicio_id() != null) {
            Servicio s = servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
            if (s != null) base = s.getPrecio_base();
        }
        if (base == null) base = BigDecimal.ZERO;
        BigDecimal sumaExtras = extras.stream().map(CitaCostoAdicional::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("base", base);
        r.put("extras", extras);
        r.put("total_extras", sumaExtras);
        r.put("total", base.add(sumaExtras));
        r.put("editable", motivoBloqueo(cita) == null);
        return ResponseEntity.ok(r);
    }

    @PostMapping
    public ResponseEntity<?> agregar(@PathVariable Integer citaId, @RequestBody Map<String, Object> b) {
        Cita cita = citaRepositorio.findById(citaId).orElse(null);
        if (cita == null) return ResponseEntity.notFound().build();
        String prohibido = validarPermiso(cita, b.get("usuario_id"));
        if (prohibido != null) return error(HttpStatus.FORBIDDEN, prohibido);

        String concepto = b.get("concepto") == null ? "" : b.get("concepto").toString().trim();
        BigDecimal monto;
        try {
            monto = new BigDecimal(b.get("monto").toString());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "Monto inválido.");
        }
        if (concepto.isBlank()) return error(HttpStatus.BAD_REQUEST, "Describe el concepto del costo.");
        if (monto.signum() <= 0) return error(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a 0.");

        CitaCostoAdicional c = new CitaCostoAdicional();
        c.setCita_id(citaId);
        c.setConcepto(concepto);
        c.setMonto(monto);
        c.setAgregado_por(Integer.valueOf(b.get("usuario_id").toString()));
        return ResponseEntity.ok(repositorio.save(c));
    }

    @DeleteMapping("/{costoId}")
    public ResponseEntity<?> quitar(@PathVariable Integer citaId, @PathVariable Integer costoId, @RequestParam Integer usuarioId) {
        Cita cita = citaRepositorio.findById(citaId).orElse(null);
        CitaCostoAdicional c = repositorio.findById(costoId).orElse(null);
        if (cita == null || c == null || !citaId.equals(c.getCita_id())) return ResponseEntity.notFound().build();
        String prohibido = validarPermiso(cita, usuarioId);
        if (prohibido != null) return error(HttpStatus.FORBIDDEN, prohibido);
        repositorio.delete(c);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private String motivoBloqueo(Cita cita) {
        if ("Cancelada".equals(cita.getEstado()) || "No asistió".equals(cita.getEstado())) {
            return "La cita está " + cita.getEstado() + "; ya no admite costos adicionales.";
        }
        boolean cobrada = pagoRepositorio.findAll().stream().anyMatch(p -> cita.getId().equals(p.getCita_id()));
        if (cobrada) return "La cita ya fue cobrada; ya no admite costos adicionales.";
        return null;
    }

    private String validarPermiso(Cita cita, Object usuarioId) {
        Usuario u = null;
        try {
            if (usuarioId != null) u = usuarioRepositorio.findById(Integer.valueOf(usuarioId.toString())).orElse(null);
        } catch (NumberFormatException ignorado) { }
        if (u == null || (!Integer.valueOf(3).equals(u.getRol_id()) && !Integer.valueOf(1).equals(u.getRol_id()))) {
            return "Solo el veterinario (o el Administrador) puede modificar los costos de una cita.";
        }
        if (Integer.valueOf(3).equals(u.getRol_id()) && cita.getVeterinario_id() != null && !cita.getVeterinario_id().equals(u.getId())) {
            return "Esta cita está asignada a otro veterinario.";
        }
        return motivoBloqueo(cita);
    }

    private ResponseEntity<?> error(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg));
    }
}

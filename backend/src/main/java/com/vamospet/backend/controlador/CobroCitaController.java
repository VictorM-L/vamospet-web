package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
public class CobroCitaController {

    @Autowired private CitaRepository citaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private CitaCostoAdicionalRepository costoRepositorio;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private ComprobanteRepository comprobanteRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;

    @PostMapping("/{id}/cobrar")
    @Transactional
    public synchronized ResponseEntity<?> cobrar(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        Cita cita = citaRepositorio.findById(id).orElse(null);
        if (cita == null) return ResponseEntity.notFound().build();

        Usuario cobrador = null;
        try {
            if (b.get("usuario_id") != null) cobrador = usuarioRepositorio.findById(Integer.valueOf(b.get("usuario_id").toString())).orElse(null);
        } catch (NumberFormatException ignorado) { }
        if (cobrador == null || (!Integer.valueOf(1).equals(cobrador.getRol_id()) && !Integer.valueOf(4).equals(cobrador.getRol_id()))) {
            return error(HttpStatus.FORBIDDEN, "Solo el Administrador o el Auxiliar pueden cobrar citas.");
        }
        if (!"Atendida".equals(cita.getEstado())) return error(HttpStatus.BAD_REQUEST, "Solo se cobran citas Atendidas.");
        if (pagoRepositorio.findAll().stream().anyMatch(p -> id.equals(p.getCita_id()))) {
            return error(HttpStatus.CONFLICT, "Esta cita ya fue cobrada.");
        }
        String metodo = b.get("metodo") == null ? "" : b.get("metodo").toString().trim();
        if (metodo.isBlank()) return error(HttpStatus.BAD_REQUEST, "Elige el método de pago.");

        Mascota mascota = cita.getMascota_id() == null ? null : mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
        if (mascota == null || mascota.getUsuario_id() == null) return error(HttpStatus.BAD_REQUEST, "La cita no tiene un cliente asociado.");

        BigDecimal base = cita.getCosto();
        if (base == null && cita.getServicio_id() != null) {
            Servicio s = servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
            if (s != null) base = s.getPrecio_base();
        }
        if (base == null) base = BigDecimal.ZERO;
        BigDecimal extras = costoRepositorio.findAll().stream().filter(c -> id.equals(c.getCita_id()))
                .map(CitaCostoAdicional::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = base.add(extras);

        Pago pago = new Pago();
        pago.setUsuario_id(mascota.getUsuario_id());
        pago.setCita_id(id);
        pago.setMonto(total);
        pago.setMetodo(metodo);
        pago.setId_registrado_por(cobrador.getId());
        pago = pagoRepositorio.save(pago);

        Comprobante comp = new Comprobante();
        comp.setPago_id(pago.getId());
        comp.setTipo("Boleta");
        comp.setNumero("B001-C" + id);
        comp.setCodigo_verificacion(UUID.randomUUID().toString());
        comp = comprobanteRepositorio.save(comp);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pago", pago);
        r.put("comprobante", comp);
        return ResponseEntity.ok(r);
    }

    private ResponseEntity<?> error(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg));
    }
}

package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.servicio.CitaService;
import com.vamospet.backend.servicio.AgendaService;
import com.vamospet.backend.servicio.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "*")
public class ServicioController {

    @Autowired
    private ServicioRepository repositorio;

    @Autowired
    private CitaRepository citaRepositorio;

    @GetMapping
    public List<Servicio> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos,
                                 @RequestParam(required = false) String tipo) {
        return repositorio.findAll().stream()
                .filter(x -> incluirInactivos || !Boolean.FALSE.equals(x.getActivo()))
                .filter(x -> tipo == null || tipo.equalsIgnoreCase(x.getTipo()))
                .toList();
    }

    @PostMapping
    public Servicio guardar(@RequestBody Servicio servicio) {
        Servicio previo = servicio.getId() == null ? null : repositorio.findById(servicio.getId()).orElse(null);
        if (previo != null) {
            if (servicio.getDuracion_min() == null) servicio.setDuracion_min(previo.getDuracion_min());
            if (servicio.getCupo_diario() == null) servicio.setCupo_diario(previo.getCupo_diario());
            if (servicio.getTipo() == null) servicio.setTipo(previo.getTipo());
        }
        if (servicio.getDuracion_min() == null) servicio.setDuracion_min(AgendaService.DURACION_CITA_MINUTOS);
        int d = servicio.getDuracion_min();
        if (d < 15 || d > 480 || d % 15 != 0) {
            throw new NegocioException(400, "La duración debe estar entre 15 y 480 minutos, en múltiplos de 15.");
        }
        if (servicio.getCupo_diario() != null && (servicio.getCupo_diario() < 1 || servicio.getCupo_diario() > 100)) {
            throw new NegocioException(400, "El cupo diario debe estar entre 1 y 100.");
        }
        if (servicio.getPrecio_base() == null || servicio.getPrecio_base().signum() < 0) {
            throw new NegocioException(400, "Indica un precio válido.");
        }
        if (!Boolean.FALSE.equals(servicio.getActivo()) && servicio.getPrecio_base().signum() == 0) {
            throw new NegocioException(400, "Pon un precio mayor a 0 antes de activar el servicio.");
        }
        if (servicio.getTipo() == null || servicio.getTipo().isBlank()) servicio.setTipo("Cita");
        return repositorio.save(servicio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Servicio existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        long pendientes = citaRepositorio.findAll().stream()
                .filter(c -> id.equals(c.getServicio_id()) && CitaService.ACTIVAS.contains(c.getEstado())
                        && c.getFecha_hora() != null && c.getFecha_hora().isAfter(LocalDateTime.now()))
                .count();
        if (pendientes > 0) {
            throw new NegocioException(409, "No se puede dar de baja: hay " + pendientes + " cita(s) por atender con este servicio. Reprográmalas o cancélalas primero.");
        }
        existente.setActivo(false);
        repositorio.save(existente);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Servicio existente = repositorio.findById(id).orElse(null);
        if (existente == null) return ResponseEntity.notFound().build();
        if (existente.getPrecio_base() == null || existente.getPrecio_base().signum() <= 0) {
            throw new NegocioException(400, "Pon un precio mayor a 0 antes de activar el servicio.");
        }
        existente.setActivo(true);
        return ResponseEntity.ok(repositorio.save(existente));
    }
}

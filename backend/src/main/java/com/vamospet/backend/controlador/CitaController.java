package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.repositorio.VeterinarioEspecialidadRepository;
import com.vamospet.backend.servicio.AgendaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
public class CitaController {
    
    @Autowired
    private CitaRepository repositorio;

    @Autowired
    private MascotaRepository mascotaRepositorio;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private VeterinarioEspecialidadRepository vetEspRepositorio;

    @Autowired
    private ServicioRepository servicioRepositorio;

    @Autowired
    private com.vamospet.backend.servicio.ExamenService examenService;

    @GetMapping
    public List<Cita> listar() { return repositorio.findAll(); }

    @GetMapping("/mascota/{mascotaId}")
    public List<Cita> listarPorMascota(@PathVariable Integer mascotaId) {
        return repositorio.findAll().stream()
                .filter(c -> c.getMascota_id() != null && c.getMascota_id().equals(mascotaId))
                .toList();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Cita> listarPorUsuario(@PathVariable Integer usuarioId) {
        List<Integer> idsMascotas = mascotaRepositorio.buscarPorDueno(usuarioId).stream()
                .map(Mascota::getId)
                .toList();
        return repositorio.findAll().stream()
                .filter(c -> c.getMascota_id() != null && idsMascotas.contains(c.getMascota_id()))
                .toList();
    }

    @PostMapping
    public synchronized ResponseEntity<?> agendar(@RequestBody Cita cita) {
        if (cita.getMascota_id() == null || cita.getFecha_hora() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan la mascota o la fecha y hora."));
        }
        if (cita.getVeterinario_id() != null) {
            if (cita.getEspecialidad_id() != null) {
                boolean tiene = vetEspRepositorio.findAll().stream().anyMatch(v ->
                        cita.getVeterinario_id().equals(v.getVeterinario_id()) && cita.getEspecialidad_id().equals(v.getEspecialidad_id()));
                if (!tiene) return ResponseEntity.badRequest().body(Map.of("error", "Ese veterinario no atiende esa especialidad."));
            }
            if (!agendaService.franjaLibre(cita.getVeterinario_id(), cita.getFecha_hora())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Esa franja ya no está disponible. Elige otra."));
            }
        }
        cita.setId(null);
        Servicio servicio = cita.getServicio_id() == null ? null : servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
        boolean quirurgica = com.vamospet.backend.servicio.ExamenService.esServicioQuirurgico(servicio);
        if (quirurgica && examenService.aprobacionVigente(cita.getMascota_id(), cita.getFecha_hora()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                    com.vamospet.backend.servicio.ExamenService.MENSAJE_SIN_APROBACION));
        }
        if (cita.getCosto() == null && servicio != null) cita.setCosto(servicio.getPrecio_base());
        Cita guardada = repositorio.save(cita);
        if (quirurgica) examenService.exigirYRegistrarCirugiaDeCita(guardada, servicio);
        return ResponseEntity.ok(guardada);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Integer id, @RequestBody Cita datos) {
        Cita citaExistente = repositorio.findById(id).orElse(null);
        if (citaExistente == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "La cita no existe."));
        if (!ESTADOS_VALIDOS.contains(datos.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado de cita inválido."));
        }
        if ("Atendida".equals(datos.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Para marcar una cita como Atendida registra la atención (diagnóstico y tratamiento)."));
        }
        if ("Atendida".equals(citaExistente.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Una cita atendida ya no puede cambiar de estado."));
        }

        citaExistente.setEstado(datos.getEstado());
        Cita guardada = repositorio.save(citaExistente);

        Mascota mascota = mascotaRepositorio.findById(guardada.getMascota_id()).orElse(null);
        if (mascota != null && mascota.getUsuario_id() != null) {
            String mensaje;
            switch (guardada.getEstado()) {
                case "Confirmada" -> mensaje = "Tu cita fue confirmada. ¡Te esperamos!";
                case "Atendida" -> mensaje = "Tu mascota ya fue atendida. Gracias por confiar en Vamos Pet.";
                case "No asistió" -> mensaje = "Registramos que no asististe a tu cita. Puedes agendar una nueva cuando quieras.";
                case "Cancelada" -> mensaje = "Tu cita fue cancelada. Si fue un error, contáctanos.";
                default -> mensaje = "El estado de tu cita cambió a: " + guardada.getEstado();
            }
            notificacionService.notificar(mascota.getUsuario_id(), "Actualización de tu cita", mensaje, "Cita");
        }

        String referencia = "cita:" + guardada.getId();
        notificacionService.cancelarProgramadas(referencia);
        if ("Confirmada".equals(guardada.getEstado()) && mascota != null && mascota.getUsuario_id() != null
                && guardada.getFecha_hora() != null && guardada.getFecha_hora().isAfter(java.time.LocalDateTime.now())) {
            java.time.LocalDateTime cuando = guardada.getFecha_hora().minusHours(24);
            if (cuando.isBefore(java.time.LocalDateTime.now())) cuando = java.time.LocalDateTime.now();
            String cuandoTexto = guardada.getFecha_hora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm"));
            notificacionService.programar(mascota.getUsuario_id(), "Recordatorio de cita",
                    "Te recordamos la cita de " + mascota.getNombre() + " el " + cuandoTexto + ". ¡Te esperamos en Vamos Pet!",
                    "Cita", cuando, referencia);
        }

        return ResponseEntity.ok(guardada);
    }

    private static final java.util.Set<String> ESTADOS_VALIDOS =
            java.util.Set.of("Pendiente", "Confirmada", "Atendida", "No asistió", "Cancelada");
}
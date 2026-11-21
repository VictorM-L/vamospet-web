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
import com.vamospet.backend.servicio.AutorizacionService;
import com.vamospet.backend.servicio.CitaService;
import com.vamospet.backend.permisos.Rol;
import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.modelo.HistorialEvento;
import com.vamospet.backend.repositorio.HistorialEventoRepository;
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
    private AutorizacionService autorizacion;

    @Autowired
    private HistorialEventoRepository historialRepositorio;

    @Autowired
    private com.vamospet.backend.servicio.ExamenService examenService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private com.vamospet.backend.repositorio.UsuarioRepository usuarioRepositorio;

    @GetMapping
    public List<Cita> listar() {
        List<Cita> todas = repositorio.findAll();
        if (!UsuarioActual.es(Rol.VETERINARIO)) return todas;
        return todas.stream().map(CitaController::sinMontos).toList();
    }

    static Cita sinMontos(Cita c) {
        Cita copia = new Cita();
        copia.setId(c.getId());
        copia.setMascota_id(c.getMascota_id());
        copia.setServicio_id(c.getServicio_id());
        copia.setEspecialidad_id(c.getEspecialidad_id());
        copia.setVeterinario_id(c.getVeterinario_id());
        copia.setFecha_hora(c.getFecha_hora());
        copia.setEstado(c.getEstado());
        copia.setEnlace_videollamada(c.getEnlace_videollamada());
        copia.setMotivo_estado(c.getMotivo_estado());
        copia.setCancelacion_solicitada(c.getCancelacion_solicitada());
        copia.setMotivo_solicitud(c.getMotivo_solicitud());
        copia.setCosto(null);
        return copia;
    }

    @GetMapping("/mascota/{mascotaId}")
    public List<Cita> listarPorMascota(@PathVariable Integer mascotaId) {
        boolean vet = UsuarioActual.es(Rol.VETERINARIO);
        return repositorio.findAll().stream()
                .filter(c -> c.getMascota_id() != null && c.getMascota_id().equals(mascotaId))
                .map(c -> vet ? sinMontos(c) : c)
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
        Servicio servicio = cita.getServicio_id() == null ? null : servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
        if (cita.getServicio_id() != null && servicio == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El servicio elegido no existe."));
        }
        if (servicio != null && Boolean.FALSE.equals(servicio.getActivo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese servicio no está disponible por ahora."));
        }
        if (servicio != null && !SERVICIOS_AGENDABLES.contains(String.valueOf(servicio.getTipo()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ese servicio se reserva desde su propia sección, no como cita."));
        }
        boolean grooming = servicio != null && TIPO_GROOMING.equals(servicio.getTipo());
        if (grooming) {
            Usuario groomer = cita.getVeterinario_id() == null ? null : usuarioRepositorio.findById(cita.getVeterinario_id()).orElse(null);
            if (groomer == null || !Boolean.TRUE.equals(groomer.getHace_grooming()) || Boolean.FALSE.equals(groomer.getActivo())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Elige un veterinario habilitado para grooming."));
            }
            cita.setEspecialidad_id(null);
        }
        if (cita.getVeterinario_id() != null) {
            if (cita.getEspecialidad_id() != null) {
                boolean tiene = vetEspRepositorio.findAll().stream().anyMatch(v ->
                        cita.getVeterinario_id().equals(v.getVeterinario_id()) && cita.getEspecialidad_id().equals(v.getEspecialidad_id()));
                if (!tiene) return ResponseEntity.badRequest().body(Map.of("error", "Ese veterinario no atiende esa especialidad."));
            }
            int duracion = AgendaService.duracionDe(servicio);
            if (!agendaService.franjaLibre(cita.getVeterinario_id(), cita.getFecha_hora(), duracion, null)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                        "Ese horario ya no alcanza para un servicio de " + duracion + " minutos. Elige otro."));
            }
        }
        if (UsuarioActual.es(Rol.CLIENTE)) {
            Mascota propia = mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
            if (propia == null || !UsuarioActual.id().equals(propia.getUsuario_id())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puedes agendar citas para tus mascotas."));
            }
        }
        cita.setId(null);
        cita.setEstado(CitaService.PENDIENTE);
        cita.setMotivo_estado(null);
        cita.setCancelacion_solicitada(null);
        cita.setMotivo_solicitud(null);
        cita.setEnlace_videollamada(null);
        cita.setInicio_real(null);
        cita.setFin_real(null);
        boolean quirurgica = com.vamospet.backend.servicio.ExamenService.esServicioQuirurgico(servicio);
        if (quirurgica && examenService.aprobacionVigente(cita.getMascota_id(), cita.getFecha_hora()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                    com.vamospet.backend.servicio.ExamenService.MENSAJE_SIN_APROBACION));
        }
        cita.setCosto(servicio != null && servicio.getPrecio_base() != null ? servicio.getPrecio_base() : java.math.BigDecimal.ZERO);
        Cita guardada = repositorio.save(cita);
        if (quirurgica) examenService.exigirYRegistrarCirugiaDeCita(guardada, servicio);
        return ResponseEntity.ok(guardada);
    }

    private static final String TIPO_GROOMING = "Grooming";
    private static final java.util.Set<String> SERVICIOS_AGENDABLES = java.util.Set.of("Cita", TIPO_GROOMING);


    @PatchMapping("/{id}/videollamada")
    public ResponseEntity<?> enviarVideollamada(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        Object uid = b.get("usuario_id");
        Integer usuarioId;
        try { usuarioId = uid == null ? null : Integer.valueOf(uid.toString()); } catch (NumberFormatException e) { usuarioId = null; }
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO);
        if (quien == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo el veterinario asignado o el administrador pueden enviar el enlace."));
        Cita cita = repositorio.findById(id).orElse(null);
        if (cita == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "La cita no existe."));
        if (quien.getRol_id() == AutorizacionService.VETERINARIO && !quien.getId().equals(cita.getVeterinario_id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Esta cita está asignada a otro veterinario."));
        }
        Servicio servicio = cita.getServicio_id() == null ? null : servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
        if (servicio == null || servicio.getNombre() == null || !servicio.getNombre().toLowerCase().contains("virtual")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo las consultas virtuales llevan enlace de videollamada."));
        }
        if (!"Confirmada".equals(cita.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Confirma la cita antes de enviar el enlace."));
        }
        String enlace = b.get("enlace") == null ? "" : b.get("enlace").toString().trim();
        if (enlace.length() > 500 || !enlace.matches("(?i)^https://[^\\s<>\"']+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingresa un enlace válido que empiece con https://"));
        }
        cita.setEnlace_videollamada(enlace);
        repositorio.save(cita);
        Mascota mascota = mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
        if (mascota != null) {
            HistorialEvento h = new HistorialEvento();
            h.setMascota_id(mascota.getId());
            h.setTipo_evento("Videollamada");
            h.setFecha(java.time.LocalDateTime.now());
            h.setDescripcion("Enlace de tu consulta virtual del " + cita.getFecha_hora().toLocalDate() + " a las "
                    + cita.getFecha_hora().toLocalTime().withSecond(0).withNano(0) + ": " + enlace);
            h.setVeterinario_id(cita.getVeterinario_id());
            historialRepositorio.save(h);
            if (mascota.getUsuario_id() != null) {
                notificacionService.notificar(mascota.getUsuario_id(), "Enlace de tu consulta virtual",
                        "Tu veterinario envió el enlace para la videollamada de " + mascota.getNombre() + " (" + cita.getFecha_hora().toLocalDate()
                                + " " + cita.getFecha_hora().toLocalTime().withSecond(0).withNano(0) + "): " + enlace, "Cita");
            }
        }
        return ResponseEntity.ok(Map.of("mensaje", "Enlace enviado al cliente.", "enlace", enlace));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(citaService.cambiarEstadoAuxiliar(id, texto(b, "estado"), texto(b, "motivo")));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Integer id) {
        return ResponseEntity.ok(sinMontos(citaService.confirmar(UsuarioActual.id(), id)));
    }

    @PatchMapping("/{id}/no-puedo-atender")
    public ResponseEntity<?> noPuedoAtender(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(sinMontos(citaService.noPuedoAtender(UsuarioActual.id(), id, texto(b, "motivo"))));
    }

    @GetMapping("/{id}/veterinarios-disponibles")
    public ResponseEntity<?> veterinariosDisponibles(@PathVariable Integer id) {
        return ResponseEntity.ok(citaService.veterinariosDisponibles(id).stream()
                .map(u -> Map.of("id", u.getId(), "nombre", u.getNombre() == null ? "" : u.getNombre()))
                .toList());
    }

    @PatchMapping("/{id}/reasignar")
    public ResponseEntity<?> reasignar(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(citaService.reasignar(id, entero(b, "veterinario_id")));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarPorCliente(@PathVariable Integer id) {
        return ResponseEntity.ok(citaService.cancelarPorCliente(UsuarioActual.id(), id));
    }

    @PostMapping("/{id}/solicitud-cancelacion")
    public ResponseEntity<?> solicitarCancelacion(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(citaService.solicitarCancelacion(UsuarioActual.id(), id, texto(b, "motivo")));
    }

    @PatchMapping("/{id}/solicitud-cancelacion/resolver")
    public ResponseEntity<?> resolverSolicitud(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        return ResponseEntity.ok(citaService.resolverSolicitud(id, Boolean.parseBoolean(String.valueOf(b.get("aprobar")))));
    }

    private static String texto(Map<String, Object> b, String clave) {
        Object v = b.get(clave);
        return v == null ? null : v.toString();
    }

    private static Integer entero(Map<String, Object> b, String clave) {
        Object v = b.get(clave);
        try {
            return v == null || v.toString().isBlank() ? null : Integer.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

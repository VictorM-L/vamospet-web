package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.repositorio.VeterinarioEspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CitaService {

    public static final String PENDIENTE = "Pendiente";
    public static final String CONFIRMADA = "Confirmada";
    public static final String ATENDIDA = "Atendida";
    public static final String NO_ASISTIO = "No asistió";
    public static final String CANCELADA = "Cancelada";
    public static final String RECHAZADA = "Rechazada";
    public static final String POR_REASIGNAR = "Por reasignar";

    public static final Set<String> LIBERAN_FRANJA = Set.of(CANCELADA, RECHAZADA, POR_REASIGNAR);
    public static final Set<String> ACTIVAS = Set.of(PENDIENTE, CONFIRMADA, POR_REASIGNAR);

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm");

    @Autowired private CitaRepository citaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private VeterinarioEspecialidadRepository vetEspRepositorio;
    @Autowired private NotificacionService notificaciones;
    @Autowired private ExamenService examenService;
    @Autowired private AgendaService agendaService;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private ConfiguracionService configuracion;

    public Cita confirmar(Integer veterinarioId, Integer citaId) {
        Cita cita = buscar(citaId);
        exigirAsignada(cita, veterinarioId);
        if (!PENDIENTE.equals(cita.getEstado())) {
            throw new NegocioException(409, "Solo se confirman citas pendientes. Esta cita está " + cita.getEstado() + ".");
        }
        cita.setMotivo_estado(null);
        return aplicar(cita, CONFIRMADA, "Tu cita del " + cuando(cita) + " fue confirmada. ¡Te esperamos!");
    }

    public Cita noPuedoAtender(Integer veterinarioId, Integer citaId, String motivo) {
        Cita cita = buscar(citaId);
        exigirAsignada(cita, veterinarioId);
        if (!PENDIENTE.equals(cita.getEstado()) && !CONFIRMADA.equals(cita.getEstado())) {
            throw new NegocioException(409, "Solo puedes liberar citas pendientes o confirmadas.");
        }
        cita.setMotivo_estado(motivoObligatorio(motivo, "Explica brevemente por qué no puedes atender."));
        Cita guardada = aplicar(cita, POR_REASIGNAR,
                "Estamos reasignando tu cita del " + cuando(cita) + " a otro veterinario. Te avisaremos apenas quede lista.");
        avisarAuxiliares("Cita por reasignar",
                "La cita #" + cita.getId() + " del " + cuando(cita) + " quedó por reasignar. Motivo del veterinario: " + cita.getMotivo_estado());
        return guardada;
    }

    public Cita cambiarEstadoAuxiliar(Integer citaId, String estado, String motivo) {
        Cita cita = buscar(citaId);
        String actual = cita.getEstado();
        if (ATENDIDA.equals(actual)) throw new NegocioException(409, "Una cita atendida ya no puede cambiar de estado.");
        if (estado == null) throw new NegocioException(400, "Indica el nuevo estado.");
        switch (estado) {
            case RECHAZADA -> {
                if (!PENDIENTE.equals(actual)) throw new NegocioException(409, "Solo se rechazan citas pendientes.");
                cita.setMotivo_estado(motivoObligatorio(motivo, "Indica el motivo del rechazo; se lo enviaremos al cliente."));
                limpiarSolicitud(cita);
                return aplicar(cita, RECHAZADA, "Tu cita del " + cuando(cita) + " no pudo ser aceptada. Motivo: "
                        + cita.getMotivo_estado() + ". Puedes agendar otra fecha cuando quieras.");
            }
            case CANCELADA -> {
                if (!ACTIVAS.contains(actual)) throw new NegocioException(409, "Esta cita ya no se puede cancelar.");
                cita.setMotivo_estado(textoOpcional(motivo));
                limpiarSolicitud(cita);
                return aplicar(cita, CANCELADA, "Tu cita del " + cuando(cita) + " fue cancelada. Si fue un error, contáctanos.");
            }
            case NO_ASISTIO -> {
                if (!CONFIRMADA.equals(actual)) throw new NegocioException(409, "Solo se marca \"No asistió\" en citas confirmadas.");
                limpiarSolicitud(cita);
                return aplicar(cita, NO_ASISTIO, "Registramos que no asististe a tu cita del " + cuando(cita)
                        + ". Puedes agendar una nueva cuando quieras.");
            }
            case ATENDIDA -> throw new NegocioException(400,
                    "Para marcar una cita como Atendida, el veterinario registra la atención (diagnóstico y tratamiento).");
            default -> throw new NegocioException(400, "El auxiliar solo puede rechazar, cancelar o marcar No asistió.");
        }
    }

    public List<Usuario> veterinariosDisponibles(Integer citaId) {
        Cita cita = buscar(citaId);
        if (cita.getFecha_hora() == null || !cita.getFecha_hora().isAfter(LocalDateTime.now())) return List.of();
        boolean grooming = cita.getServicio_id() != null && servicioRepositorio.findById(cita.getServicio_id())
                .map(sv -> "Grooming".equals(sv.getTipo())).orElse(false);
        int duracion = agendaService.duracionDe(cita.getServicio_id());
        return usuarioRepositorio.findAll().stream()
                .filter(u -> Integer.valueOf(AutorizacionService.VETERINARIO).equals(u.getRol_id()))
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .filter(u -> !u.getId().equals(cita.getVeterinario_id()) || !POR_REASIGNAR.equals(cita.getEstado()))
                .filter(u -> !grooming || Boolean.TRUE.equals(u.getHace_grooming()))
                .filter(u -> atiendeEspecialidad(u.getId(), cita.getEspecialidad_id()))
                .filter(u -> agendaService.franjaLibre(u.getId(), cita.getFecha_hora(), duracion, cita.getId()))
                .toList();
    }

    public Cita reasignar(Integer citaId, Integer veterinarioId) {
        Cita cita = buscar(citaId);
        if (!POR_REASIGNAR.equals(cita.getEstado())) {
            throw new NegocioException(409, "Solo se reasignan citas que están por reasignar.");
        }
        if (veterinarioId == null) throw new NegocioException(400, "Elige el veterinario.");
        Usuario vet = veterinariosDisponibles(citaId).stream()
                .filter(u -> u.getId().equals(veterinarioId)).findFirst()
                .orElseThrow(() -> new NegocioException(409, "Ese veterinario no está libre a esa hora o no atiende esa especialidad."));
        cita.setVeterinario_id(vet.getId());
        cita.setMotivo_estado(null);
        Cita guardada = aplicar(cita, PENDIENTE, "Tu cita del " + cuando(cita) + " ahora la atenderá " + vet.getNombre()
                + ". Te avisaremos cuando la confirme.");
        notificaciones.notificar(vet.getId(), "Nueva cita asignada",
                "Se te asignó la cita #" + cita.getId() + " del " + cuando(cita) + ". Revísala y confírmala.", "Cita");
        return guardada;
    }

    public Cita cancelarPorCliente(Integer clienteId, Integer citaId) {
        Cita cita = buscar(citaId);
        exigirDueno(cita, clienteId);
        if (!ACTIVAS.contains(cita.getEstado())) throw new NegocioException(409, "Esta cita ya no se puede cancelar.");
        if (!conAnticipacion(cita)) {
            throw new NegocioException(409, "Faltan menos de " + configuracion.horasCancelacion()
                    + " horas para tu cita. Usa \"Solicitar cancelación\" y la clínica te responderá.");
        }
        limpiarSolicitud(cita);
        cita.setMotivo_estado("Cancelada por el cliente");
        return aplicar(cita, CANCELADA, "Cancelaste tu cita del " + cuando(cita) + ". Puedes agendar otra cuando quieras.");
    }

    public Cita solicitarCancelacion(Integer clienteId, Integer citaId, String motivo) {
        Cita cita = buscar(citaId);
        exigirDueno(cita, clienteId);
        if (!ACTIVAS.contains(cita.getEstado())) throw new NegocioException(409, "Esta cita ya no se puede cancelar.");
        if (conAnticipacion(cita)) {
            throw new NegocioException(409, "Aún faltan " + configuracion.horasCancelacion() + " horas o más: puedes cancelarla tú mismo.");
        }
        if (cita.getCancelacion_solicitada() != null) {
            throw new NegocioException(409, "Ya enviaste una solicitud de cancelación para esta cita.");
        }
        cita.setCancelacion_solicitada(LocalDateTime.now());
        cita.setMotivo_solicitud(motivoObligatorio(motivo, "Cuéntanos el motivo de la cancelación."));
        Cita guardada = citaRepositorio.save(cita);
        avisarAuxiliares("Solicitud de cancelación",
                "El cliente pidió cancelar la cita #" + cita.getId() + " del " + cuando(cita) + ". Motivo: " + cita.getMotivo_solicitud());
        return guardada;
    }

    public Cita resolverSolicitud(Integer citaId, boolean aprobar) {
        Cita cita = buscar(citaId);
        if (cita.getCancelacion_solicitada() == null) {
            throw new NegocioException(409, "Esta cita no tiene una solicitud de cancelación pendiente.");
        }
        if (aprobar) {
            if (!ACTIVAS.contains(cita.getEstado())) throw new NegocioException(409, "Esta cita ya no se puede cancelar.");
            cita.setMotivo_estado("Cancelación tardía aprobada: " + cita.getMotivo_solicitud());
            limpiarSolicitud(cita);
            return aplicar(cita, CANCELADA, "Aprobamos tu solicitud: la cita del " + cuando(cita) + " quedó cancelada.");
        }
        limpiarSolicitud(cita);
        Cita guardada = citaRepositorio.save(cita);
        avisarCliente(guardada, "Tu cita del " + cuando(cita) + " se mantiene. Si no puedes asistir, comunícate con la clínica.");
        return guardada;
    }

    public boolean conAnticipacion(Cita cita) {
        if (cita.getFecha_hora() == null) return false;
        return Duration.between(LocalDateTime.now(), cita.getFecha_hora()).toMinutes() >= configuracion.horasCancelacion() * 60L;
    }

    private Cita aplicar(Cita cita, String nuevoEstado, String mensajeCliente) {
        cita.setEstado(nuevoEstado);
        Cita guardada = citaRepositorio.save(cita);
        examenService.sincronizarConCita(guardada.getId(), guardada.getEstado());
        String referencia = "cita:" + guardada.getId();
        notificaciones.cancelarProgramadas(referencia);
        Mascota mascota = guardada.getMascota_id() == null ? null : mascotaRepositorio.findById(guardada.getMascota_id()).orElse(null);
        if (mascota != null && mascota.getUsuario_id() != null) {
            notificaciones.notificar(mascota.getUsuario_id(), "Actualización de tu cita", mensajeCliente, "Cita");
            if (CONFIRMADA.equals(nuevoEstado) && guardada.getFecha_hora() != null && guardada.getFecha_hora().isAfter(LocalDateTime.now())) {
                LocalDateTime aviso = guardada.getFecha_hora().minusHours(24);
                if (aviso.isBefore(LocalDateTime.now())) aviso = LocalDateTime.now();
                notificaciones.programar(mascota.getUsuario_id(), "Recordatorio de cita",
                        "Te recordamos la cita de " + mascota.getNombre() + " el " + cuando(guardada) + ". ¡Te esperamos en Vamos Pet!",
                        "Cita", aviso, referencia);
            }
        }
        return guardada;
    }

    private void avisarCliente(Cita cita, String mensaje) {
        Mascota mascota = cita.getMascota_id() == null ? null : mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
        if (mascota != null && mascota.getUsuario_id() != null) {
            notificaciones.notificar(mascota.getUsuario_id(), "Actualización de tu cita", mensaje, "Cita");
        }
    }

    private void avisarAuxiliares(String titulo, String mensaje) {
        for (Usuario u : usuarioRepositorio.findAll()) {
            if (Integer.valueOf(AutorizacionService.AUXILIAR).equals(u.getRol_id()) && !Boolean.FALSE.equals(u.getActivo())) {
                notificaciones.notificar(u.getId(), titulo, mensaje, "Cita");
            }
        }
    }

    private Cita buscar(Integer citaId) {
        return citaRepositorio.findById(citaId).orElseThrow(() -> new NegocioException(404, "La cita no existe."));
    }

    private void exigirAsignada(Cita cita, Integer veterinarioId) {
        if (veterinarioId == null || !veterinarioId.equals(cita.getVeterinario_id())) {
            throw new NegocioException(403, "Esta cita está asignada a otro veterinario.");
        }
    }

    private void exigirDueno(Cita cita, Integer clienteId) {
        Mascota mascota = cita.getMascota_id() == null ? null : mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
        if (mascota == null || clienteId == null || !Objects.equals(mascota.getUsuario_id(), clienteId)) {
            throw new NegocioException(403, "Solo puedes gestionar las citas de tus mascotas.");
        }
    }

    private boolean atiendeEspecialidad(Integer veterinarioId, Integer especialidadId) {
        if (especialidadId == null) return true;
        return vetEspRepositorio.findAll().stream().anyMatch(v ->
                veterinarioId.equals(v.getVeterinario_id()) && especialidadId.equals(v.getEspecialidad_id()));
    }

    private static void limpiarSolicitud(Cita cita) {
        cita.setCancelacion_solicitada(null);
        cita.setMotivo_solicitud(null);
    }

    private static String motivoObligatorio(String motivo, String mensaje) {
        String limpio = textoOpcional(motivo);
        if (limpio == null || limpio.length() < 5) throw new NegocioException(400, mensaje);
        return limpio;
    }

    private static String textoOpcional(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) return null;
        return limpio.length() > 300 ? limpio.substring(0, 300) : limpio;
    }

    private static String cuando(Cita cita) {
        return cita.getFecha_hora() == null ? "—" : cita.getFecha_hora().format(FORMATO);
    }
}

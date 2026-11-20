package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AtencionMedicaService {

    public static class AtencionException extends RuntimeException {
        private final int estado;
        public AtencionException(int estado, String mensaje) { super(mensaje); this.estado = estado; }
        public int getEstado() { return estado; }
    }

    @Autowired private CitaRepository citaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private HistorialEventoRepository historialRepositorio;
    @Autowired private VacunaRepository vacunaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private NotificacionService notificacionService;
    @Autowired private ExamenService examenService;

    public record DatosAtencion(String diagnostico, String tratamiento, String vacunaNombre, LocalDate vacunaProximaDosis) {}

    @Transactional
    public Cita atender(Integer citaId, Integer usuarioId, DatosAtencion datos) {
        Usuario quien = autorizacion.conRol(usuarioId, AutorizacionService.ADMIN, AutorizacionService.VETERINARIO);
        if (quien == null) throw new AtencionException(403, "Solo el veterinario o el administrador pueden registrar una atención.");

        Cita cita = citaId == null ? null : citaRepositorio.findById(citaId).orElse(null);
        if (cita == null) throw new AtencionException(404, "La cita no existe.");
        if (!"Confirmada".equals(cita.getEstado())) {
            throw new AtencionException(409, "Solo se puede atender una cita Confirmada (estado actual: " + cita.getEstado() + ").");
        }
        if (quien.getRol_id() == AutorizacionService.VETERINARIO
                && cita.getVeterinario_id() != null && !cita.getVeterinario_id().equals(quien.getId())) {
            throw new AtencionException(403, "Esta cita está asignada a otro veterinario.");
        }

        String diagnostico = limpiar(datos.diagnostico());
        String tratamiento = limpiar(datos.tratamiento());
        String vacuna = limpiar(datos.vacunaNombre());
        if (diagnostico == null) throw new AtencionException(400, "El diagnóstico es obligatorio.");
        if (diagnostico.length() > 2000 || (tratamiento != null && tratamiento.length() > 2000)) {
            throw new AtencionException(400, "El diagnóstico y el tratamiento admiten hasta 2000 caracteres.");
        }
        if (vacuna != null && vacuna.length() > 100) throw new AtencionException(400, "El nombre de la vacuna es demasiado largo.");
        if (vacuna == null && datos.vacunaProximaDosis() != null) {
            throw new AtencionException(400, "Indicaste una próxima dosis pero no la vacuna aplicada.");
        }
        if (datos.vacunaProximaDosis() != null && !datos.vacunaProximaDosis().isAfter(LocalDate.now())) {
            throw new AtencionException(400, "La próxima dosis debe ser una fecha futura.");
        }

        Mascota mascota = mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
        if (mascota == null) throw new AtencionException(404, "La mascota de la cita no existe.");
        Servicio servicio = cita.getServicio_id() == null ? null : servicioRepositorio.findById(cita.getServicio_id()).orElse(null);

        Integer vetId = cita.getVeterinario_id() != null ? cita.getVeterinario_id() : quien.getId();
        LocalDateTime ahora = LocalDateTime.now();

        StringBuilder texto = new StringBuilder();
        if (servicio != null) texto.append("Servicio: ").append(servicio.getNombre()).append('\n');
        Usuario vet = usuarioRepositorio.findById(vetId).orElse(null);
        if (vet != null) texto.append("Atendido por: ").append(vet.getNombre()).append('\n');
        texto.append("Diagnóstico: ").append(diagnostico);
        if (tratamiento != null) texto.append("\nTratamiento / indicaciones: ").append(tratamiento);
        registrarEvento(mascota.getId(), "Consulta", ahora, texto.toString(), vetId);

        if (vacuna != null) {
            Vacuna v = new Vacuna();
            v.setMascota_id(mascota.getId());
            v.setNombre(vacuna);
            v.setFecha_aplicacion(LocalDate.now());
            v.setProxima_dosis(datos.vacunaProximaDosis());
            vacunaRepositorio.save(v);
            registrarEvento(mascota.getId(), "Vacunación", ahora,
                    "Vacuna aplicada: " + vacuna + (datos.vacunaProximaDosis() != null ? ". Próxima dosis: " + datos.vacunaProximaDosis() : ""), vetId);
        }

        cita.setEstado("Atendida");
        Cita guardada = citaRepositorio.save(cita);
        notificacionService.cancelarProgramadas("cita:" + guardada.getId());
        examenService.sincronizarConCita(guardada.getId(), "Atendida");
        if (mascota.getUsuario_id() != null) {
            notificacionService.notificar(mascota.getUsuario_id(), "Atención registrada",
                    mascota.getNombre() + " fue atendido. Puedes ver el detalle en su historial médico.", "Cita");
        }
        return guardada;
    }

    private void registrarEvento(Integer mascotaId, String tipo, LocalDateTime fecha, String descripcion, Integer vetId) {
        HistorialEvento e = new HistorialEvento();
        e.setMascota_id(mascotaId);
        e.setTipo_evento(tipo);
        e.setFecha(fecha);
        e.setDescripcion(descripcion);
        e.setVeterinario_id(vetId);
        historialRepositorio.save(e);
    }

    private static String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

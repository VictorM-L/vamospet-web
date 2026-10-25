package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.HorarioAtencion;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HorarioAtencionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    public static final int DURACION_CITA_MINUTOS = 30;

    @Autowired
    private HorarioAtencionRepository horarioRepositorio;

    @Autowired
    private CitaRepository citaRepositorio;

    public List<LocalTime> franjasLibres(Integer veterinarioId, LocalDate fecha) {
        List<LocalTime> libres = new ArrayList<>();
        if (fecha.isBefore(LocalDate.now())) return libres;
        int dia = fecha.getDayOfWeek().getValue();

        List<LocalTime> ocupadas = citaRepositorio.findAll().stream()
                .filter(c -> veterinarioId.equals(c.getVeterinario_id()))
                .filter(c -> c.getFecha_hora() != null && c.getFecha_hora().toLocalDate().equals(fecha))
                .filter(c -> !"Cancelada".equals(c.getEstado()))
                .map(c -> c.getFecha_hora().toLocalTime().withSecond(0).withNano(0))
                .toList();

        LocalDateTime ahora = LocalDateTime.now();
        for (HorarioAtencion h : horarioRepositorio.findAll()) {
            if (!veterinarioId.equals(h.getVeterinario_id()) || h.getDia_semana() == null || h.getDia_semana() != dia) continue;
            LocalTime t = h.getHora_inicio();
            while (!t.plusMinutes(DURACION_CITA_MINUTOS).isAfter(h.getHora_fin())) {
                boolean pasada = fecha.equals(LocalDate.now()) && !LocalDateTime.of(fecha, t).isAfter(ahora);
                if (!pasada && !ocupadas.contains(t)) libres.add(t);
                t = t.plusMinutes(DURACION_CITA_MINUTOS);
            }
        }
        libres.sort(LocalTime::compareTo);
        return libres;
    }

    public boolean franjaLibre(Integer veterinarioId, LocalDateTime fechaHora) {
        LocalTime hora = fechaHora.toLocalTime().withSecond(0).withNano(0);
        return franjasLibres(veterinarioId, fechaHora.toLocalDate()).contains(hora);
    }

    public boolean tieneHorario(Integer veterinarioId) {
        return horarioRepositorio.findAll().stream().anyMatch(h -> veterinarioId.equals(h.getVeterinario_id()));
    }
}

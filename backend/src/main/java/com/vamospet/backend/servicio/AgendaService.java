package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.HorarioAtencion;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HorarioAtencionRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class AgendaService {

    public static final int DURACION_CITA_MINUTOS = 30;
    public static final int PASO_MINUTOS = 30;

    @Autowired
    private HorarioAtencionRepository horarioRepositorio;

    @Autowired
    private CitaRepository citaRepositorio;

    @Autowired
    private ServicioRepository servicioRepositorio;

    public static boolean solapa(int inicioA, int finA, int inicioB, int finB) {
        return inicioA < finB && inicioB < finA;
    }

    public int duracionDe(Integer servicioId) {
        if (servicioId == null) return DURACION_CITA_MINUTOS;
        return servicioRepositorio.findById(servicioId).map(AgendaService::duracionDe).orElse(DURACION_CITA_MINUTOS);
    }

    public static int duracionDe(Servicio s) {
        return s == null || s.getDuracion_min() == null || s.getDuracion_min() <= 0 ? DURACION_CITA_MINUTOS : s.getDuracion_min();
    }

    public List<LocalTime> franjasLibres(Integer veterinarioId, LocalDate fecha) {
        return franjasLibres(veterinarioId, fecha, DURACION_CITA_MINUTOS, null);
    }

    public List<LocalTime> franjasLibres(Integer veterinarioId, LocalDate fecha, int duracionMinutos, Integer ignorarCitaId) {
        List<LocalTime> libres = new ArrayList<>();
        if (fecha.isBefore(LocalDate.now())) return libres;
        int duracion = duracionMinutos <= 0 ? DURACION_CITA_MINUTOS : duracionMinutos;
        int dia = fecha.getDayOfWeek().getValue();

        List<Cita> delDia = citaRepositorio.findAll().stream()
                .filter(c -> veterinarioId.equals(c.getVeterinario_id()))
                .filter(c -> c.getFecha_hora() != null && c.getFecha_hora().toLocalDate().equals(fecha))
                .filter(c -> !CitaService.LIBERAN_FRANJA.contains(c.getEstado()))
                .filter(c -> ignorarCitaId == null || !ignorarCitaId.equals(c.getId()))
                .toList();
        Map<Integer, Integer> duraciones = new HashMap<>();
        if (delDia.stream().anyMatch(c -> c.getServicio_id() != null)) {
            for (Servicio s : servicioRepositorio.findAll()) duraciones.put(s.getId(), duracionDe(s));
        }
        List<int[]> ocupados = new ArrayList<>();
        for (Cita c : delDia) {
            int inicio = minutos(c.getFecha_hora().toLocalTime());
            int dur = c.getServicio_id() == null ? DURACION_CITA_MINUTOS : duraciones.getOrDefault(c.getServicio_id(), DURACION_CITA_MINUTOS);
            ocupados.add(new int[]{inicio, inicio + dur});
        }

        LocalDateTime ahora = LocalDateTime.now();
        TreeSet<LocalTime> resultado = new TreeSet<>();
        for (HorarioAtencion h : horarioRepositorio.findAll()) {
            if (!veterinarioId.equals(h.getVeterinario_id()) || h.getDia_semana() == null || h.getDia_semana() != dia) continue;
            int fin = minutos(h.getHora_fin());
            for (int t = minutos(h.getHora_inicio()); t + duracion <= fin; t += PASO_MINUTOS) {
                LocalTime hora = LocalTime.of(t / 60, t % 60);
                boolean pasada = fecha.equals(LocalDate.now()) && !LocalDateTime.of(fecha, hora).isAfter(ahora);
                if (pasada) continue;
                final int ini = t;
                boolean choca = ocupados.stream().anyMatch(o -> solapa(ini, ini + duracion, o[0], o[1]));
                if (!choca) resultado.add(hora);
            }
        }
        libres.addAll(resultado);
        return libres;
    }

    public boolean franjaLibre(Integer veterinarioId, LocalDateTime fechaHora) {
        return franjaLibre(veterinarioId, fechaHora, DURACION_CITA_MINUTOS, null);
    }

    public boolean franjaLibre(Integer veterinarioId, LocalDateTime fechaHora, int duracionMinutos, Integer ignorarCitaId) {
        LocalTime hora = fechaHora.toLocalTime().withSecond(0).withNano(0);
        return franjasLibres(veterinarioId, fechaHora.toLocalDate(), duracionMinutos, ignorarCitaId).contains(hora);
    }

    public boolean tieneHorario(Integer veterinarioId) {
        return horarioRepositorio.findAll().stream().anyMatch(h -> veterinarioId.equals(h.getVeterinario_id()));
    }

    private static int minutos(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}

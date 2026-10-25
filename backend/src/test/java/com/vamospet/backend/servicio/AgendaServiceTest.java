package com.vamospet.backend.servicio;
import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.HorarioAtencion;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HorarioAtencionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {
    private static final Integer VET = 7;
    @Mock private HorarioAtencionRepository horarioRepositorio;
    @Mock private CitaRepository citaRepositorio;
    @InjectMocks private AgendaService agenda;
    private LocalDate proximoLunes;
    @BeforeEach
    void preparar() {
        proximoLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
    private HorarioAtencion horario(int vet, int dia, String inicio, String fin) {
        HorarioAtencion h = new HorarioAtencion();
        h.setVeterinario_id(vet);
        h.setDia_semana(dia);
        h.setHora_inicio(LocalTime.parse(inicio));
        h.setHora_fin(LocalTime.parse(fin));
        return h;
    }
    private Cita cita(int vet, LocalDateTime fechaHora, String estado) {
        Cita c = new Cita();
        c.setVeterinario_id(vet);
        c.setFecha_hora(fechaHora);
        c.setEstado(estado);
        return c;
    }
    @Test
    @DisplayName("Un bloque de 2 horas genera 4 franjas de 30 minutos")
    void generaFranjasDeTreintaMinutos() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 1, "09:00", "11:00")));
        when(citaRepositorio.findAll()).thenReturn(List.of());
        List<LocalTime> libres = agenda.franjasLibres(VET, proximoLunes);
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30)), libres);
    }
    @Test
    @DisplayName("Una cita activa ocupa su franja")
    void excluyeFranjaOcupada() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 1, "09:00", "11:00")));
        when(citaRepositorio.findAll()).thenReturn(List.of(cita(VET, proximoLunes.atTime(9, 30), "Confirmada")));
        List<LocalTime> libres = agenda.franjasLibres(VET, proximoLunes);
        assertEquals(3, libres.size());
        assertFalse(libres.contains(LocalTime.of(9, 30)));
    }
    @Test
    @DisplayName("Una cita cancelada libera la franja")
    void citaCanceladaNoBloquea() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 1, "09:00", "10:00")));
        when(citaRepositorio.findAll()).thenReturn(List.of(cita(VET, proximoLunes.atTime(9, 0), "Cancelada")));
        assertTrue(agenda.franjasLibres(VET, proximoLunes).contains(LocalTime.of(9, 0)));
    }
    @Test
    @DisplayName("La cita de otro veterinario no afecta la agenda")
    void citaDeOtroVeterinarioNoBloquea() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 1, "09:00", "10:00")));
        when(citaRepositorio.findAll()).thenReturn(List.of(cita(99, proximoLunes.atTime(9, 0), "Pendiente")));
        assertEquals(2, agenda.franjasLibres(VET, proximoLunes).size());
    }
    @Test
    @DisplayName("Si el veterinario no atiende ese día de la semana no hay franjas")
    void sinFranjasEnDiaSinHorario() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 2, "09:00", "13:00")));
        when(citaRepositorio.findAll()).thenReturn(List.of());
        assertTrue(agenda.franjasLibres(VET, proximoLunes).isEmpty());
    }
    @Test
    @DisplayName("Una fecha pasada no devuelve franjas (y no consulta la BD)")
    void fechaPasadaDevuelveVacio() {
        assertTrue(agenda.franjasLibres(VET, LocalDate.now().minusDays(1)).isEmpty());
    }
    @Test
    @DisplayName("franjaLibre: true si la hora está libre, false si está ocupada")
    void franjaLibreSegunDisponibilidad() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 1, "09:00", "10:00")));
        when(citaRepositorio.findAll()).thenReturn(List.of(cita(VET, proximoLunes.atTime(9, 0), "Pendiente")));
        assertFalse(agenda.franjaLibre(VET, proximoLunes.atTime(9, 0)));
        assertTrue(agenda.franjaLibre(VET, proximoLunes.atTime(9, 30)));
    }
    @Test
    @DisplayName("tieneHorario detecta si el veterinario tiene al menos un bloque")
    void tieneHorario() {
        when(horarioRepositorio.findAll()).thenReturn(List.of(horario(VET, 1, "09:00", "10:00")));
        assertTrue(agenda.tieneHorario(VET));
        assertFalse(agenda.tieneHorario(123));
    }
}

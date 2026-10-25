package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtencionMedicaServiceTest {

    @Mock private CitaRepository citaRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private HistorialEventoRepository historialRepositorio;
    @Mock private VacunaRepository vacunaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private AutorizacionService autorizacion;
    @Mock private NotificacionService notificacionService;
    @InjectMocks private AtencionMedicaService servicio;

    private Usuario vet;
    private Cita cita;

    @BeforeEach
    void preparar() {
        vet = new Usuario();
        vet.setId(4);
        vet.setNombre("Dr. Clark");
        vet.setRol_id(3);
        cita = new Cita();
        cita.setId(10);
        cita.setMascota_id(7);
        cita.setVeterinario_id(4);
        cita.setEstado("Confirmada");
        cita.setFecha_hora(LocalDateTime.now());
    }

    private void conVeterinario() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet);
        when(citaRepositorio.findById(10)).thenReturn(Optional.of(cita));
    }

    private void conMascota() {
        Mascota m = new Mascota();
        m.setId(7);
        m.setNombre("Firulais");
        m.setUsuario_id(2);
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(m));
        when(usuarioRepositorio.findById(4)).thenReturn(Optional.of(vet));
        when(citaRepositorio.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Atender guarda el diagnóstico en el historial, marca la cita Atendida y avisa al dueño")
    void atenderGuardaHistorial() {
        conVeterinario();
        conMascota();
        Cita r = servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("Otitis leve", "Limpieza cada 12 h", null, null));
        assertEquals("Atendida", r.getEstado());
        ArgumentCaptor<HistorialEvento> ev = ArgumentCaptor.forClass(HistorialEvento.class);
        verify(historialRepositorio, times(1)).save(ev.capture());
        assertEquals("Consulta", ev.getValue().getTipo_evento());
        assertEquals(7, ev.getValue().getMascota_id());
        assertTrue(ev.getValue().getDescripcion().contains("Otitis leve"));
        assertTrue(ev.getValue().getDescripcion().contains("Dr. Clark"));
        verify(vacunaRepositorio, never()).save(any());
        verify(notificacionService).notificar(eq2(2), anyS(), anyS(), eq2("Cita"));
    }

    @Test
    @DisplayName("Con vacuna se crea el registro de vacuna y un evento de vacunación")
    void atenderConVacuna() {
        conVeterinario();
        conMascota();
        LocalDate proxima = LocalDate.now().plusDays(365);
        servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("Control", null, "Quíntuple", proxima));
        ArgumentCaptor<Vacuna> v = ArgumentCaptor.forClass(Vacuna.class);
        verify(vacunaRepositorio).save(v.capture());
        assertEquals("Quíntuple", v.getValue().getNombre());
        assertEquals(proxima, v.getValue().getProxima_dosis());
        assertEquals(LocalDate.now(), v.getValue().getFecha_aplicacion());
        verify(historialRepositorio, times(2)).save(any(HistorialEvento.class));
    }

    @Test
    @DisplayName("El diagnóstico es obligatorio y no se guarda nada si falta")
    void diagnosticoObligatorio() {
        conVeterinario();
        AtencionMedicaService.AtencionException ex = assertThrows(AtencionMedicaService.AtencionException.class,
                () -> servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("  ", "x", null, null)));
        assertEquals(400, ex.getEstado());
        verifyNoInteractions(historialRepositorio, vacunaRepositorio);
        assertEquals("Confirmada", cita.getEstado());
    }

    @Test
    @DisplayName("Solo se atiende una cita Confirmada")
    void soloConfirmada() {
        cita.setEstado("Pendiente");
        conVeterinario();
        assertEquals(409, assertThrows(AtencionMedicaService.AtencionException.class,
                () -> servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("x", null, null, null))).getEstado());
    }

    @Test
    @DisplayName("Un veterinario no puede atender la cita de otro")
    void citaDeOtroVeterinario() {
        cita.setVeterinario_id(99);
        conVeterinario();
        assertEquals(403, assertThrows(AtencionMedicaService.AtencionException.class,
                () -> servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("x", null, null, null))).getEstado());
    }

    @Test
    @DisplayName("Un cliente o auxiliar no puede registrar atenciones")
    void sinPermiso() {
        when(autorizacion.conRol(5, 1, 3)).thenReturn(null);
        assertEquals(403, assertThrows(AtencionMedicaService.AtencionException.class,
                () -> servicio.atender(10, 5, new AtencionMedicaService.DatosAtencion("x", null, null, null))).getEstado());
    }

    @Test
    @DisplayName("La próxima dosis debe ser futura y requiere el nombre de la vacuna")
    void validacionesDeVacuna() {
        conVeterinario();
        assertEquals(400, assertThrows(AtencionMedicaService.AtencionException.class,
                () -> servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("x", null, null, LocalDate.now().plusDays(5)))).getEstado());
        assertEquals(400, assertThrows(AtencionMedicaService.AtencionException.class,
                () -> servicio.atender(10, 4, new AtencionMedicaService.DatosAtencion("x", null, "Rabia", LocalDate.now().minusDays(1)))).getEstado());
    }

    private static Integer eq2(Integer v) { return org.mockito.ArgumentMatchers.eq(v); }
    private static String eq2(String v) { return org.mockito.ArgumentMatchers.eq(v); }
    private static String anyS() { return org.mockito.ArgumentMatchers.anyString(); }
}
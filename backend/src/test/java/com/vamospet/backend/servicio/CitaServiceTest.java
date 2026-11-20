package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.modelo.VeterinarioEspecialidad;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.repositorio.VeterinarioEspecialidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final int VET = 18, OTRO_VET = 24, CLIENTE = 20, AUX = 19, MASCOTA = 5;

    @Mock private CitaRepository citaRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private VeterinarioEspecialidadRepository vetEspRepositorio;
    @Mock private NotificacionService notificaciones;
    @Mock private ExamenService examenService;
    @Mock private AgendaService agendaService;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private ConfiguracionService configuracion;
    @InjectMocks private CitaService servicio;

    @BeforeEach
    void umbral() {
        lenient().when(configuracion.horasCancelacion()).thenReturn(24);
    }

    private Cita cita(String estado, LocalDateTime cuando) {
        Cita c = new Cita();
        c.setId(1);
        c.setMascota_id(MASCOTA);
        c.setVeterinario_id(VET);
        c.setEspecialidad_id(2);
        c.setEstado(estado);
        c.setFecha_hora(cuando);
        return c;
    }

    private Mascota mascota() {
        Mascota m = new Mascota();
        m.setId(MASCOTA);
        m.setUsuario_id(CLIENTE);
        m.setNombre("Firulais");
        return m;
    }

    private Usuario usuario(int id, int rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        u.setNombre("Persona " + id);
        u.setActivo(true);
        return u;
    }

    private void existe(Cita c) {
        when(citaRepositorio.findById(1)).thenReturn(Optional.of(c));
    }

    private void guardaYDuenio() {
        when(citaRepositorio.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
    }

    private static LocalDateTime enHoras(int horas) {
        return LocalDateTime.now().plusHours(horas);
    }

    @Test
    @DisplayName("El veterinario confirma con un clic su cita pendiente y se programa el recordatorio")
    void confirmar() {
        existe(cita("Pendiente", enHoras(48)));
        guardaYDuenio();
        Cita r = servicio.confirmar(VET, 1);
        assertEquals("Confirmada", r.getEstado());
        verify(notificaciones).notificar(eq(CLIENTE), anyString(), anyString(), eq("Cita"));
        verify(notificaciones).programar(eq(CLIENTE), eq("Recordatorio de cita"), anyString(), eq("Cita"), any(), eq("cita:1"));
    }

    @Test
    @DisplayName("Un veterinario no confirma la cita de otro (403)")
    void confirmarAjena() {
        existe(cita("Pendiente", enHoras(48)));
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.confirmar(OTRO_VET, 1));
        assertEquals(403, e.getEstado());
        verify(citaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Solo se confirma lo pendiente (409)")
    void confirmarNoPendiente() {
        existe(cita("Cancelada", enHoras(48)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.confirmar(VET, 1)).getEstado());
    }

    @Test
    @DisplayName("\"No puedo atender\" exige motivo")
    void noPuedoSinMotivo() {
        existe(cita("Confirmada", enHoras(48)));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.noPuedoAtender(VET, 1, "  ")).getEstado());
    }

    @Test
    @DisplayName("\"No puedo atender\" deja la cita por reasignar y avisa a cada Auxiliar activo")
    void noPuedoAtender() {
        existe(cita("Confirmada", enHoras(48)));
        guardaYDuenio();
        Usuario auxInactivo = usuario(30, 4);
        auxInactivo.setActivo(false);
        when(usuarioRepositorio.findAll()).thenReturn(List.of(usuario(AUX, 4), auxInactivo, usuario(VET, 3), usuario(1, 1)));
        Cita r = servicio.noPuedoAtender(VET, 1, "Tengo una cirugía de emergencia");
        assertEquals("Por reasignar", r.getEstado());
        assertEquals("Tengo una cirugía de emergencia", r.getMotivo_estado());
        verify(notificaciones).notificar(eq(AUX), eq("Cita por reasignar"), anyString(), eq("Cita"));
        verify(notificaciones, never()).notificar(eq(30), anyString(), anyString(), anyString());
        verify(notificaciones).cancelarProgramadas("cita:1");
    }

    @Test
    @DisplayName("El Auxiliar solo rechaza citas pendientes y con motivo")
    void rechazar() {
        existe(cita("Confirmada", enHoras(48)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoAuxiliar(1, "Rechazada", "Sin cupo")).getEstado());
    }

    @Test
    @DisplayName("Rechazar una pendiente guarda el motivo y avisa al cliente")
    void rechazarPendiente() {
        existe(cita("Pendiente", enHoras(48)));
        guardaYDuenio();
        Cita r = servicio.cambiarEstadoAuxiliar(1, "Rechazada", "El servicio no está disponible ese día");
        assertEquals("Rechazada", r.getEstado());
        verify(examenService).sincronizarConCita(1, "Rechazada");
    }

    @Test
    @DisplayName("\"No asistió\" solo aplica a citas confirmadas")
    void noAsistioSoloConfirmada() {
        existe(cita("Pendiente", enHoras(-1)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoAuxiliar(1, "No asistió", null)).getEstado());
    }

    @Test
    @DisplayName("Una cita atendida ya no cambia de estado")
    void atendidaEsFinal() {
        existe(cita("Atendida", enHoras(-5)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoAuxiliar(1, "Cancelada", null)).getEstado());
    }

    @Test
    @DisplayName("El cliente cancela solo si faltan 24 horas o más")
    void clienteCancelaConAnticipacion() {
        existe(cita("Confirmada", enHoras(30)));
        guardaYDuenio();
        assertEquals("Cancelada", servicio.cancelarPorCliente(CLIENTE, 1).getEstado());
    }

    @Test
    @DisplayName("Con menos de 24 horas el cliente no puede cancelar directamente (409)")
    void clienteCancelaTarde() {
        existe(cita("Confirmada", enHoras(10)));
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cancelarPorCliente(CLIENTE, 1)).getEstado());
        verify(citaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Un cliente no gestiona la cita de otra persona (403)")
    void clienteAjeno() {
        existe(cita("Confirmada", enHoras(30)));
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.cancelarPorCliente(99, 1)).getEstado());
    }

    @Test
    @DisplayName("Con menos de 24 horas el cliente envía una solicitud que llega al Auxiliar")
    void solicitudTardia() {
        existe(cita("Confirmada", enHoras(10)));
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
        when(citaRepositorio.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepositorio.findAll()).thenReturn(List.of(usuario(AUX, 4)));
        Cita r = servicio.solicitarCancelacion(CLIENTE, 1, "Mi mascota está mejor");
        assertNotNull(r.getCancelacion_solicitada());
        assertEquals("Confirmada", r.getEstado());
        verify(notificaciones).notificar(eq(AUX), eq("Solicitud de cancelación"), anyString(), eq("Cita"));
    }

    @Test
    @DisplayName("Con 24 horas o más no hace falta solicitud: el cliente cancela solo")
    void solicitudNoNecesaria() {
        existe(cita("Confirmada", enHoras(48)));
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.solicitarCancelacion(CLIENTE, 1, "Viaje imprevisto")).getEstado());
    }

    @Test
    @DisplayName("No se envían dos solicitudes para la misma cita")
    void solicitudRepetida() {
        Cita c = cita("Confirmada", enHoras(10));
        c.setCancelacion_solicitada(LocalDateTime.now().minusMinutes(5));
        existe(c);
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.solicitarCancelacion(CLIENTE, 1, "Otra vez")).getEstado());
    }

    @Test
    @DisplayName("El Auxiliar aprueba la solicitud y la cita queda cancelada")
    void aprobarSolicitud() {
        Cita c = cita("Confirmada", enHoras(10));
        c.setCancelacion_solicitada(LocalDateTime.now());
        c.setMotivo_solicitud("Mi mascota está mejor");
        existe(c);
        guardaYDuenio();
        Cita r = servicio.resolverSolicitud(1, true);
        assertEquals("Cancelada", r.getEstado());
        assertNull(r.getCancelacion_solicitada());
    }

    @Test
    @DisplayName("Solo se reasigna una cita que está por reasignar")
    void reasignarSoloPorReasignar() {
        existe(cita("Confirmada", enHoras(48)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.reasignar(1, OTRO_VET)).getEstado());
    }

    @Test
    @DisplayName("No se reasigna a un veterinario ocupado a esa hora")
    void reasignarOcupado() {
        Cita c = cita("Por reasignar", enHoras(48));
        existe(c);
        when(usuarioRepositorio.findAll()).thenReturn(List.of(usuario(VET, 3), usuario(OTRO_VET, 3)));
        when(vetEspRepositorio.findAll()).thenReturn(List.of(especialidad(OTRO_VET, 2)));
        when(agendaService.franjaLibre(eq(OTRO_VET), eq(c.getFecha_hora()), anyInt(), eq(1))).thenReturn(false);
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.reasignar(1, OTRO_VET)).getEstado());
    }

    @Test
    @DisplayName("Reasignar pasa la cita al nuevo veterinario, vuelve a Pendiente y avisa a ambos")
    void reasignar() {
        Cita c = cita("Por reasignar", enHoras(48));
        existe(c);
        guardaYDuenio();
        when(usuarioRepositorio.findAll()).thenReturn(List.of(usuario(VET, 3), usuario(OTRO_VET, 3)));
        when(vetEspRepositorio.findAll()).thenReturn(List.of(especialidad(OTRO_VET, 2)));
        when(agendaService.franjaLibre(eq(OTRO_VET), eq(c.getFecha_hora()), anyInt(), eq(1))).thenReturn(true);
        Cita r = servicio.reasignar(1, OTRO_VET);
        assertEquals(OTRO_VET, r.getVeterinario_id());
        assertEquals("Pendiente", r.getEstado());
        verify(notificaciones).notificar(eq(OTRO_VET), eq("Nueva cita asignada"), anyString(), eq("Cita"));
        verify(notificaciones).notificar(eq(CLIENTE), anyString(), anyString(), eq("Cita"));
    }

    @Test
    @DisplayName("Una cita de grooming solo se reasigna a veterinarios que hacen grooming y con la duración del paquete")
    void reasignarGrooming() {
        Cita c = cita("Por reasignar", enHoras(48));
        c.setEspecialidad_id(null);
        c.setServicio_id(40);
        existe(c);
        Servicio paquete = new Servicio();
        paquete.setId(40);
        paquete.setTipo("Grooming");
        paquete.setDuracion_min(120);
        when(servicioRepositorio.findById(40)).thenReturn(Optional.of(paquete));
        when(agendaService.duracionDe(40)).thenReturn(120);
        Usuario sinGrooming = usuario(OTRO_VET, 3);
        Usuario groomer = usuario(12, 3);
        groomer.setHace_grooming(true);
        when(usuarioRepositorio.findAll()).thenReturn(List.of(usuario(VET, 3), sinGrooming, groomer));
        when(agendaService.franjaLibre(12, c.getFecha_hora(), 120, 1)).thenReturn(true);
        List<Usuario> libres = servicio.veterinariosDisponibles(1);
        assertEquals(List.of(12), libres.stream().map(Usuario::getId).toList());
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.reasignar(1, OTRO_VET)).getEstado());
    }

    @Test
    @DisplayName("El umbral de cancelación sale de la configuración de la clínica")
    void umbralConfigurable() {
        Cita c = cita("Confirmada", enHoras(30));
        assertTrue(servicio.conAnticipacion(c));
        when(configuracion.horasCancelacion()).thenReturn(48);
        assertFalse(servicio.conAnticipacion(c));
        existe(c);
        when(mascotaRepositorio.findById(MASCOTA)).thenReturn(Optional.of(mascota()));
        NegocioException ex = assertThrows(NegocioException.class, () -> servicio.cancelarPorCliente(CLIENTE, 1));
        assertEquals(409, ex.getEstado());
        assertTrue(ex.getMessage().contains("48 horas"));
    }

    private VeterinarioEspecialidad especialidad(int vet, int esp) {
        VeterinarioEspecialidad v = new VeterinarioEspecialidad();
        v.setVeterinario_id(vet);
        v.setEspecialidad_id(esp);
        return v;
    }
}

package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamenServiceTest {

    @Mock private OrdenExamenRepository ordenRepositorio;
    @Mock private ArchivoExamenRepository archivoRepositorio;
    @Mock private CirugiaProgramadaRepository cirugiaRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private CitaRepository citaRepositorio;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private HistorialEventoRepository historialRepositorio;
    @Mock private RecetaRepository recetaRepositorio;
    @Mock private AutorizacionService autorizacion;
    @Mock private AgendaService agendaService;
    @Mock private NotificacionService notificacionService;
    @InjectMocks private ExamenService servicio;

    private Usuario vet(int id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(3);
        u.setNombre("Vet " + id);
        return u;
    }

    private OrdenExamen orden(String estado, LocalDateTime evaluada) {
        OrdenExamen o = new OrdenExamen();
        o.setId(1);
        o.setMascota_id(7);
        o.setVeterinario_id(4);
        o.setTipo_examen("Hemograma");
        o.setCirugia_prevista("Esterilización");
        o.setEstado(estado);
        o.setFecha_evaluacion(evaluada);
        return o;
    }

    @Test
    @DisplayName("La aprobación vale 30 días desde la evaluación, ni antes ni después")
    void vigencia() {
        LocalDateTime eval = LocalDateTime.now().minusDays(10);
        OrdenExamen o = orden("Apto_Cirugia", eval);
        assertTrue(ExamenService.vigenteEn(o, eval.plusDays(30)));
        assertFalse(ExamenService.vigenteEn(o, eval.plusDays(30).plusMinutes(1)));
        assertFalse(ExamenService.vigenteEn(o, eval.minusDays(1)));
        assertFalse(ExamenService.vigenteEn(orden("Pendiente", null), LocalDateTime.now()));
    }

    @Test
    @DisplayName("Reconoce los servicios quirúrgicos aunque lleven tilde o mayúsculas")
    void servicioQuirurgico() {
        Servicio s = new Servicio();
        s.setNombre("Cirugía menor");
        assertTrue(ExamenService.esServicioQuirurgico(s));
        s.setNombre("CIRUGIA mayor");
        assertTrue(ExamenService.esServicioQuirurgico(s));
        s.setNombre("Baño y corte");
        assertFalse(ExamenService.esServicioQuirurgico(s));
        assertFalse(ExamenService.esServicioQuirurgico(null));
    }

    @Test
    @DisplayName("No se programa una cirugía si el examen no está aprobado")
    void sinAprobacionNoSeAgenda() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Resultados_Subidos", null)));
        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.agendarCirugia(4, 1, LocalDateTime.now().plusDays(3), "Esterilización"));
        assertEquals(409, ex.getEstado());
        verifyNoInteractions(citaRepositorio);
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("No se programa con una aprobación de hace más de 30 días")
    void aprobacionVencida() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Apto_Cirugia", LocalDateTime.now().minusDays(31))));
        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.agendarCirugia(4, 1, LocalDateTime.now().plusDays(1), "Esterilización"));
        assertEquals(409, ex.getEstado());
        assertTrue(ex.getMessage().contains("venció"));
        verifyNoInteractions(citaRepositorio);
    }

    @Test
    @DisplayName("Con aprobación vigente y franja libre se crea la cita y la cirugía")
    void agendaConAprobacion() {
        LocalDateTime fecha = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Apto_Cirugia", LocalDateTime.now().minusDays(2))));
        when(cirugiaRepositorio.findAll()).thenReturn(List.of());
        when(agendaService.tieneHorario(4)).thenReturn(true);
        when(agendaService.franjaLibre(4, fecha)).thenReturn(true);
        when(citaRepositorio.saveAndFlush(any(Cita.class))).thenAnswer(i -> { Cita c = i.getArgument(0); c.setId(88); return c; });
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
        CirugiaProgramada c = servicio.agendarCirugia(4, 1, fecha, null);
        assertEquals(88, c.getCita_id());
        assertEquals("Esterilización", c.getTipo_cirugia());
        assertEquals("Programada", c.getEstado());
    }

    @Test
    @DisplayName("Una misma aprobación no sirve para dos cirugías")
    void aprobacionYaUsada() {
        CirugiaProgramada usada = new CirugiaProgramada();
        usada.setOrden_id(1);
        usada.setEstado("Programada");
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Apto_Cirugia", LocalDateTime.now().minusDays(1))));
        when(cirugiaRepositorio.findAll()).thenReturn(List.of(usada));
        assertEquals(409, assertThrows(NegocioException.class,
                () -> servicio.agendarCirugia(4, 1, LocalDateTime.now().plusDays(2), "x")).getEstado());
    }

    @Test
    @DisplayName("Solo el veterinario que pidió el examen (o el admin) puede evaluarlo")
    void evaluaSoloElAutorizado() {
        when(autorizacion.conRol(9, 1, 3)).thenReturn(vet(9));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Resultados_Subidos", null)));
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.evaluar(9, 1, "Apto_Cirugia", null)).getEstado());
        verify(ordenRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Rechazar exige motivo; aprobar deja fecha de evaluación")
    void evaluarReglas() {
        OrdenExamen o = orden("Resultados_Subidos", null);
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(o));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.evaluar(4, 1, "Rechazado", " ")).getEstado());
        when(ordenRepositorio.save(any(OrdenExamen.class))).thenAnswer(i -> i.getArgument(0));
        OrdenExamen r = servicio.evaluar(4, 1, "Apto_Cirugia", "Todo normal");
        assertEquals("Apto_Cirugia", r.getEstado());
        assertNotNull(r.getFecha_evaluacion());
        assertEquals(4, r.getEvaluado_por());
    }

    @Test
    @DisplayName("Solo se evalúan órdenes con resultados subidos")
    void noSeEvaluaSinResultados() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Pendiente", null)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.evaluar(4, 1, "Apto_Cirugia", null)).getEstado());
    }

    @Test
    @DisplayName("Un cliente no puede subir resultados de la mascota de otra persona")
    void clienteSoloSusMascotas() {
        Usuario cliente = new Usuario();
        cliente.setId(2);
        cliente.setRol_id(2);
        Mascota ajena = new Mascota();
        ajena.setId(7);
        ajena.setUsuario_id(3);
        when(autorizacion.conRol(2, 1, 2, 3, 4)).thenReturn(cliente);
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Pendiente", null)));
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(ajena));
        byte[] pdf = "%PDF-1.4 x".getBytes();
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.subirArchivo(2, 1, "a.pdf", pdf)).getEstado());
        verify(archivoRepositorio, never()).save(any());
    }

    private CirugiaProgramada cirugia(String estado, LocalDateTime fecha) {
        CirugiaProgramada c = new CirugiaProgramada();
        c.setId(5);
        c.setOrden_id(1);
        c.setCita_id(30);
        c.setMascota_id(7);
        c.setVeterinario_id(4);
        c.setTipo_cirugia("Esterilización");
        c.setFecha_hora(fecha);
        c.setEstado(estado);
        return c;
    }

    private Cita cita(String estado) {
        Cita c = new Cita();
        c.setId(30);
        c.setEstado(estado);
        return c;
    }

    @Test
    @DisplayName("Marcar una cirugía como realizada también atiende su cita")
    void marcarRealizada() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().minusHours(1))));
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(cita("Confirmada")));
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
        CirugiaProgramada r = servicio.cambiarEstadoCirugia(4, 5, "Realizada", "Sin complicaciones", null);
        assertEquals("Realizada", r.getEstado());
        verify(citaRepositorio).save(argThat(c -> "Atendida".equals(c.getEstado())));
        verify(historialRepositorio).save(argThat(e -> "Cirugía realizada".equals(e.getTipo_evento()) && e.getDescripcion().contains("Sin complicaciones")));
        verify(recetaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("No se marca como realizada una cirugía que todavía no llega a su fecha")
    void noRealizadaFutura() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().plusDays(1))));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoCirugia(4, 5, "Realizada", "ok", null)).getEstado());
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Cancelar una cirugía cancela su cita y libera la aprobación")
    void cancelarCirugia() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().plusDays(1))));
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(cita("Confirmada")));
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("Cancelada", servicio.cambiarEstadoCirugia(4, 5, "Cancelada", null, null).getEstado());
        verify(citaRepositorio).save(argThat(c -> "Cancelada".equals(c.getEstado())));
    }

    @Test
    @DisplayName("Solo se cambia el estado de una cirugía Programada y con un estado válido")
    void estadoInvalido() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Realizada", LocalDateTime.now().minusDays(1))));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoCirugia(4, 5, "Cancelada", null, null)).getEstado());
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoCirugia(4, 5, "Programada", null, null)).getEstado());
    }

    @Test
    @DisplayName("Otro veterinario o un cliente no pueden cambiar la cirugía")
    void sinPermisoCirugia() {
        when(autorizacion.conRol(9, 1, 3)).thenReturn(vet(9));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().minusHours(1))));
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoCirugia(9, 5, "Realizada", "ok", null)).getEstado());
        when(autorizacion.conRol(2, 1, 3)).thenReturn(null);
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoCirugia(2, 5, "Realizada", "ok", null)).getEstado());
    }

    @Test
    @DisplayName("El estado de la cita arrastra a la cirugía: atendida la realiza, cancelada o inasistencia la cancelan")
    void sincronizaConCita() {
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
        CirugiaProgramada a = cirugia("Programada", LocalDateTime.now());
        when(cirugiaRepositorio.findAll()).thenReturn(List.of(a));
        servicio.sincronizarConCita(30, "Atendida");
        assertEquals("Realizada", a.getEstado());

        CirugiaProgramada b = cirugia("Programada", LocalDateTime.now());
        when(cirugiaRepositorio.findAll()).thenReturn(List.of(b));
        servicio.sincronizarConCita(30, "No asistió");
        assertEquals("Cancelada", b.getEstado());

        servicio.sincronizarConCita(30, "Confirmada");
        verify(cirugiaRepositorio, times(2)).save(any(CirugiaProgramada.class));
    }

    @Test
    @DisplayName("Una cirugía ya cancelada vuelve a permitir usar la misma aprobación")
    void aprobacionLiberada() {
        CirugiaProgramada cancelada = cirugia("Cancelada", LocalDateTime.now().plusDays(1));
        OrdenExamen o = orden("Apto_Cirugia", LocalDateTime.now().minusDays(1));
        when(ordenRepositorio.findAll()).thenReturn(List.of(o));
        when(cirugiaRepositorio.findAll()).thenReturn(List.of(cancelada));
        assertTrue(servicio.aprobacionVigente(7, LocalDateTime.now().plusDays(2)).isPresent());
    }

    @Test
    @DisplayName("Realizar una cirugía exige el resultado")
    void resultadoObligatorio() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().minusHours(1))));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.cambiarEstadoCirugia(4, 5, "Realizada", "  ", null)).getEstado());
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Con medicamentos se crea una receta aprobada y el resultado queda en el historial")
    void recetaPostoperatoria() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().minusHours(1))));
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(cita("Confirmada")));
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
        servicio.cambiarEstadoCirugia(4, 5, "Realizada", "Todo bien", "Amoxicilina cada 12 h por 7 días");
        verify(recetaRepositorio).save(argThat(r -> r.getMascota_id() == 7 && r.getVeterinario_id() == 4
                && "Aprobada".equals(r.getEstado()) && r.getContenido().contains("Amoxicilina")));
        verify(historialRepositorio).save(argThat(e -> e.getDescripcion().contains("Todo bien") && e.getDescripcion().contains("Amoxicilina")));
    }

    @Test
    @DisplayName("Al cancelar se ignoran resultado y medicamentos y no se crea receta")
    void cancelarSinReceta() {
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet(4));
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(cirugia("Programada", LocalDateTime.now().plusDays(1))));
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(cita("Confirmada")));
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
        servicio.cambiarEstadoCirugia(4, 5, "Cancelada", "x", "y");
        verify(recetaRepositorio, never()).save(any());
    }
}

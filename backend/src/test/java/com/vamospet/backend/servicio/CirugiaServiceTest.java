package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Cita;
import com.vamospet.backend.modelo.CirugiaProgramada;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.OrdenExamen;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CirugiaProgramadaRepository;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.repositorio.HistorialEventoRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.OrdenExamenRepository;
import com.vamospet.backend.repositorio.PagoRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.repositorio.ServicioRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CirugiaServiceTest {

    private static final int VET = 4;
    private static final int OTRO_VET = 9;
    private static final int DUENO = 2;

    @Mock private CirugiaProgramadaRepository cirugiaRepositorio;
    @Mock private CitaRepository citaRepositorio;
    @Mock private OrdenExamenRepository ordenRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private RecetaRepository recetaRepositorio;
    @Mock private HistorialEventoRepository historialRepositorio;
    @Mock private PagoRepository pagoRepositorio;
    @Mock private NotificacionService notificaciones;
    @Mock private EstanciaService estanciaService;
    @Mock private CitaService citaService;
    @InjectMocks private CirugiaService servicio;

    private CirugiaProgramada cirugia(String estado, LocalDateTime fecha) {
        CirugiaProgramada c = new CirugiaProgramada();
        c.setId(5);
        c.setOrden_id(1);
        c.setCita_id(30);
        c.setMascota_id(7);
        c.setVeterinario_id(VET);
        c.setTipo_cirugia("Esterilización");
        c.setFecha_hora(fecha);
        c.setEstado(estado);
        return c;
    }

    private OrdenExamen orden(String estado, LocalDateTime evaluada) {
        OrdenExamen o = new OrdenExamen();
        o.setId(1);
        o.setMascota_id(7);
        o.setVeterinario_id(VET);
        o.setTipo_examen("Hemograma");
        o.setEstado(estado);
        o.setFecha_evaluacion(evaluada);
        return o;
    }

    private Cita cita(String estado) {
        Cita c = new Cita();
        c.setId(30);
        c.setEstado(estado);
        return c;
    }

    private Mascota mascota() {
        Mascota m = new Mascota();
        m.setId(7);
        m.setNombre("Luna");
        m.setUsuario_id(DUENO);
        return m;
    }

    private void existe(CirugiaProgramada c) {
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(c));
    }

    private void guarda() {
        when(cirugiaRepositorio.save(any(CirugiaProgramada.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Otro veterinario no puede avanzar la cirugía")
    void soloElVeterinarioAsignado() {
        existe(cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now()));
        assertEquals(403, assertThrows(NegocioException.class, () -> servicio.siguiente(OTRO_VET, 5, null)).getEstado());
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Confirmar una cirugía por confirmar confirma su cita pendiente y la deja programada")
    void confirmarConfirmaLaCita() {
        CirugiaProgramada c = cirugia(CirugiaService.POR_CONFIRMAR, LocalDateTime.now().plusDays(2));
        existe(c);
        guarda();
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(cita("Pendiente")));
        CirugiaProgramada r = servicio.siguiente(VET, 5, null);
        verify(citaService).confirmar(VET, 30);
        assertEquals(CirugiaService.PROGRAMADA, r.getEstado());
    }

    @Test
    @DisplayName("No se inicia una cirugía si el examen no está apto")
    void noIniciaSinExamenApto() {
        existe(cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now()));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Rechazado", LocalDateTime.now().minusDays(1))));
        NegocioException ex = assertThrows(NegocioException.class, () -> servicio.siguiente(VET, 5, null));
        assertEquals(409, ex.getEstado());
        verify(cirugiaRepositorio, never()).save(any());
        verifyNoInteractions(citaRepositorio);
    }

    @Test
    @DisplayName("No se inicia una cirugía con el examen vencido")
    void noIniciaConExamenVencido() {
        existe(cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now()));
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Apto_Cirugia", LocalDateTime.now().minusDays(31))));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.siguiente(VET, 5, null)).getEstado());
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("No se inicia con más de una hora de anticipación")
    void noIniciaAntesDeTiempo() {
        existe(cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now().plusHours(3)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.siguiente(VET, 5, null)).getEstado());
        verifyNoInteractions(ordenRepositorio);
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Iniciar deja la cirugía y su cita en curso y registra la hora real")
    void iniciar() {
        existe(cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now().plusMinutes(30)));
        guarda();
        when(ordenRepositorio.findById(1)).thenReturn(Optional.of(orden("Apto_Cirugia", LocalDateTime.now().minusDays(2))));
        Cita ci = cita("Confirmada");
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(ci));
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(mascota()));
        CirugiaProgramada r = servicio.siguiente(VET, 5, null);
        assertEquals(CirugiaService.EN_CURSO, r.getEstado());
        assertNotNull(r.getInicio_real());
        assertEquals("En curso", ci.getEstado());
        assertNotNull(ci.getInicio_real());
        verify(historialRepositorio).save(argThat(h -> "Cirugía iniciada".equals(h.getTipo_evento())));
        verify(notificaciones).notificar(eq(DUENO), eq("Cirugía en curso"), anyString(), anyString());
    }

    @Test
    @DisplayName("No se pasa a postoperatorio sin escribir el resultado")
    void finalizarExigeResultado() {
        existe(cirugia(CirugiaService.EN_CURSO, LocalDateTime.now()));
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.siguiente(VET, 5, new CirugiaService.Paso("  ", null, null, null, null, null, null))).getEstado());
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Finalizar crea la receta aprobada, atiende la cita y deja todo en el historial")
    void finalizarConReceta() {
        CirugiaProgramada c = cirugia(CirugiaService.EN_CURSO, LocalDateTime.now().minusHours(1));
        c.setInicio_real(LocalDateTime.now().minusMinutes(50));
        c.setNotas_intra("Sangrado leve controlado");
        existe(c);
        guarda();
        Cita ci = cita("En curso");
        when(citaRepositorio.findById(30)).thenReturn(Optional.of(ci));
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(mascota()));
        CirugiaProgramada r = servicio.siguiente(VET, 5, new CirugiaService.Paso("Sin novedades", "Ninguna", "Meloxicam 1 vez al día", null, null, null, null));
        assertEquals(CirugiaService.POSTOPERATORIO, r.getEstado());
        assertNotNull(r.getFin_real());
        assertEquals("Atendida", ci.getEstado());
        assertNotNull(ci.getFin_real());
        assertNotNull(ci.getInicio_real());
        verify(recetaRepositorio).save(argThat(rc -> rc.getMascota_id() == 7 && rc.getVeterinario_id() == VET
                && "Aprobada".equals(rc.getEstado()) && rc.getContenido().contains("Meloxicam")));
        verify(notificaciones).cancelarProgramadas("cita:30");
        verify(historialRepositorio).save(argThat(h -> "Cirugía realizada".equals(h.getTipo_evento())
                && h.getDescripcion().contains("Sin novedades") && h.getDescripcion().contains("Sangrado leve")));
    }

    @Test
    @DisplayName("Sin medicamentos no se crea receta")
    void finalizarSinReceta() {
        existe(cirugia(CirugiaService.EN_CURSO, LocalDateTime.now()));
        guarda();
        servicio.siguiente(VET, 5, new CirugiaService.Paso("Bien", null, null, null, null, null, null));
        verifyNoInteractions(recetaRepositorio);
    }

    @Test
    @DisplayName("En postoperatorio hay que decidir internar o dar el alta")
    void cerrarExigeDecision() {
        existe(cirugia(CirugiaService.POSTOPERATORIO, LocalDateTime.now()));
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.siguiente(VET, 5, CirugiaService.Paso.vacio())).getEstado());
        verify(cirugiaRepositorio, never()).save(any());
        verifyNoInteractions(estanciaService);
    }

    @Test
    @DisplayName("Internar crea el internamiento y cierra la cirugía como realizada")
    void cerrarInternando() {
        existe(cirugia(CirugiaService.POSTOPERATORIO, LocalDateTime.now()));
        guarda();
        LocalDateTime ingreso = LocalDateTime.now();
        LocalDateTime salida = ingreso.plusDays(2);
        CirugiaProgramada r = servicio.siguiente(VET, 5, new CirugiaService.Paso(null, null, null, "Internar", ingreso, salida, "Control cada 6 h"));
        verify(estanciaService).internar(VET, 5, ingreso, salida, "Control cada 6 h");
        assertEquals(CirugiaService.REALIZADA, r.getEstado());
        assertEquals("Internar", r.getPostoperatorio());
    }

    @Test
    @DisplayName("Si el internamiento falla, la cirugía sigue en postoperatorio")
    void internarFallido() {
        CirugiaProgramada c = cirugia(CirugiaService.POSTOPERATORIO, LocalDateTime.now());
        existe(c);
        when(estanciaService.internar(eq(VET), eq(5), any(), any(), any())).thenThrow(new NegocioException(409, "No hay cuartos libres"));
        assertEquals(409, assertThrows(NegocioException.class,
                () -> servicio.siguiente(VET, 5, new CirugiaService.Paso(null, null, null, "Internar", null, LocalDateTime.now().plusDays(1), null))).getEstado());
        assertEquals(CirugiaService.POSTOPERATORIO, c.getEstado());
        verify(cirugiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("El alta directa cierra la cirugía sin internamiento y avisa al dueño")
    void cerrarConAlta() {
        existe(cirugia(CirugiaService.POSTOPERATORIO, LocalDateTime.now()));
        guarda();
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(mascota()));
        CirugiaProgramada r = servicio.siguiente(VET, 5, new CirugiaService.Paso(null, null, null, "Alta directa", null, null, "Reposo 3 días"));
        verifyNoInteractions(estanciaService);
        assertEquals(CirugiaService.REALIZADA, r.getEstado());
        assertEquals("Alta directa", r.getPostoperatorio());
        verify(historialRepositorio).save(argThat(h -> "Alta directa".equals(h.getTipo_evento()) && h.getDescripcion().contains("Reposo")));
        verify(notificaciones).notificar(eq(DUENO), eq("Alta de cirugía"), anyString(), anyString());
    }

    @Test
    @DisplayName("Una cirugía realizada o cancelada no tiene más pasos")
    void sinPasosPendientes() {
        existe(cirugia(CirugiaService.REALIZADA, LocalDateTime.now().minusDays(1)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.siguiente(VET, 5, null)).getEstado());
        existe(cirugia(CirugiaService.CANCELADA, LocalDateTime.now().minusDays(1)));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.siguiente(VET, 5, null)).getEstado());
    }

    @Test
    @DisplayName("Las notas solo se guardan mientras la cirugía está en curso")
    void notasSoloEnCurso() {
        existe(cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now()));
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.guardarNotas(VET, 5, "x")).getEstado());
        existe(cirugia(CirugiaService.EN_CURSO, LocalDateTime.now()));
        guarda();
        assertEquals("Hemostasia ok", servicio.guardarNotas(VET, 5, " Hemostasia ok ").getNotas_intra());
        assertEquals(400, assertThrows(NegocioException.class, () -> servicio.guardarNotas(VET, 5, "a".repeat(2001))).getEstado());
    }

    private Usuario usuario(int id, int rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        u.setNombre("Persona " + id);
        return u;
    }

    @Test
    @DisplayName("El auxiliar ve el tablero solo con estado y cobro; el veterinario solo sus cirugías con datos clínicos")
    void tableroSegunRol() {
        CirugiaProgramada propia = cirugia(CirugiaService.POSTOPERATORIO, LocalDateTime.now());
        CirugiaProgramada ajena = cirugia(CirugiaService.PROGRAMADA, LocalDateTime.now().plusDays(1));
        ajena.setId(6);
        ajena.setVeterinario_id(OTRO_VET);
        CirugiaProgramada vieja = cirugia(CirugiaService.REALIZADA, LocalDateTime.now().minusDays(20));
        vieja.setId(8);
        vieja.setFin_real(LocalDateTime.now().minusDays(20));
        when(cirugiaRepositorio.findAll()).thenReturn(List.of(propia, ajena, vieja));
        when(usuarioRepositorio.findById(anyInt())).thenAnswer(i -> Optional.of(usuario(i.getArgument(0), i.<Integer>getArgument(0) == 50 ? 4 : 3)));
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(mascota()));
        when(pagoRepositorio.findAll()).thenReturn(List.of());

        List<Map<String, Object>> aux = servicio.tablero(50);
        assertEquals(2, aux.size());
        assertFalse(aux.get(0).containsKey("alergias"));
        assertFalse(aux.get(0).containsKey("resultado"));
        assertEquals("Por cobrar", aux.get(0).get("cobro"));

        List<Map<String, Object>> vet = servicio.tablero(VET);
        assertEquals(1, vet.size());
        assertEquals(5, vet.get(0).get("id"));
        assertTrue(vet.get(0).containsKey("alergias"));
    }
}

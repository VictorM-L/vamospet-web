package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.CirugiaProgramada;
import com.vamospet.backend.modelo.Cuarto;
import com.vamospet.backend.modelo.Estancia;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
class EstanciaGuarderiaTest {

    private static final int CLIENTE = 2;

    @Mock private EstanciaRepository estanciaRepositorio;
    @Mock private CuartoRepository cuartoRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private CirugiaProgramadaRepository cirugiaRepositorio;
    @Mock private HistorialEventoRepository historialRepositorio;
    @Mock private PagoRepository pagoRepositorio;
    @Mock private ComprobanteRepository comprobanteRepositorio;
    @Mock private AutorizacionService autorizacion;
    @Mock private PagoService pagoService;
    @Mock private NotificacionService notificacionService;
    @Mock private ConfiguracionService configuracion;
    @InjectMocks private EstanciaService servicio;

    @BeforeEach
    void horario() {
        lenient().when(configuracion.aperturaHospedaje()).thenReturn(8);
        lenient().when(configuracion.cierreHospedaje()).thenReturn(20);
    }

    private final LocalDate manana = LocalDate.now().plusDays(1);
    private final PagoService.DatosPago pago = new PagoService.DatosPago("Yape", null, null, "123456");

    private Servicio servicio(String nombre, String precio, Integer cupo) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setPrecio_base(new BigDecimal(precio));
        s.setActivo(true);
        s.setCupo_diario(cupo);
        return s;
    }

    private Estancia guarderia(int id, LocalDate dia) {
        Estancia e = new Estancia();
        e.setId(id);
        e.setMascota_id(100 + id);
        e.setTipo(EstanciaService.GUARDERIA);
        e.setIngreso(dia.atTime(9, 0));
        e.setSalida_estimada(dia.atTime(18, 0));
        e.setEstado("Reservada");
        return e;
    }

    private Estancia hospedaje(int id, int cuarto, LocalDate desde, LocalDate hasta) {
        Estancia e = new Estancia();
        e.setId(id);
        e.setMascota_id(200 + id);
        e.setCuarto_id(cuarto);
        e.setTipo(EstanciaService.HOSPEDAJE);
        e.setIngreso(desde.atTime(10, 0));
        e.setSalida_estimada(hasta.atTime(10, 0));
        e.setEstado("Reservada");
        return e;
    }

    private Cuarto cuarto(int id) {
        Cuarto c = new Cuarto();
        c.setId(id);
        c.setNumero(id);
        return c;
    }

    private void clienteConMascota() {
        Usuario u = new Usuario();
        u.setId(CLIENTE);
        u.setRol_id(2);
        when(autorizacion.conRol(CLIENTE, 2, 1, 4)).thenReturn(u);
        Mascota m = new Mascota();
        m.setId(7);
        m.setNombre("Toby");
        m.setUsuario_id(CLIENTE);
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(m));
    }

    private void guardaYCobra() {
        when(estanciaRepositorio.save(any(Estancia.class))).thenAnswer(i -> { Estancia e = i.getArgument(0); e.setId(900); return e; });
        when(pagoService.cobrar(eq(CLIENTE), eq("estancia"), anyInt(), any())).thenReturn(new PagoService.Resultado(null, null));
    }

    @Test
    @DisplayName("La guardería no usa cuartos: se reserva aunque todos estén ocupados")
    void guarderiaSinCuarto() {
        clienteConMascota();
        guardaYCobra();
        when(servicioRepositorio.findAll()).thenReturn(List.of(servicio(EstanciaService.SERVICIO_GUARDERIA, "35.00", 8)));
        when(estanciaRepositorio.findAll()).thenReturn(List.of(hospedaje(1, 1, manana, manana.plusDays(3))));
        Map<String, Object> r = servicio.reservar(CLIENTE, "Guarderia", 7, manana.atTime(9, 0), manana.plusDays(1).atTime(18, 0), " Dieta blanda ", pago);
        Estancia e = (Estancia) r.get("estancia");
        assertNull(e.getCuarto_id());
        assertEquals("Guarderia", e.getTipo());
        assertEquals("Dieta blanda", e.getCuidados_especiales());
        assertEquals(0, new BigDecimal("70.00").compareTo(e.getTotal()));
        verifyNoInteractions(cuartoRepositorio);
    }

    @Test
    @DisplayName("Cuando un día llega al cupo diario, la guardería se rechaza indicando ese día")
    void guarderiaSinCupo() {
        clienteConMascota();
        when(servicioRepositorio.findAll()).thenReturn(List.of(servicio(EstanciaService.SERVICIO_GUARDERIA, "35.00", 2)));
        when(estanciaRepositorio.findAll()).thenReturn(List.of(guarderia(1, manana.plusDays(1)), guarderia(2, manana.plusDays(1))));
        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.reservar(CLIENTE, "Guarderia", 7, manana.atTime(9, 0), manana.plusDays(2).atTime(18, 0), null, pago));
        assertEquals(409, ex.getEstado());
        assertTrue(ex.getMessage().contains(manana.plusDays(1).toString()));
        verify(estanciaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Sin precio configurado la guardería no se puede reservar")
    void guarderiaSinPrecio() {
        clienteConMascota();
        when(servicioRepositorio.findAll()).thenReturn(List.of(servicio(EstanciaService.SERVICIO_GUARDERIA, "0", 8)));
        when(estanciaRepositorio.findAll()).thenReturn(List.of());
        assertEquals(409, assertThrows(NegocioException.class,
                () -> servicio.reservar(CLIENTE, "Guarderia", 7, manana.atTime(9, 0), manana.atTime(18, 0), null, pago)).getEstado());
    }

    @Test
    @DisplayName("La guardería no ocupa cuartos del hospedaje")
    void hospedajeIgnoraGuarderia() {
        when(estanciaRepositorio.findAll()).thenReturn(List.of(guarderia(1, manana), guarderia(2, manana)));
        when(cuartoRepositorio.findAll()).thenReturn(List.of(cuarto(1)));
        assertEquals(List.of(1), servicio.cuartosLibres(manana.atTime(10, 0), manana.plusDays(1).atTime(10, 0), null));
    }

    @Test
    @DisplayName("El cupo de guardería sale del servicio, con 8 por defecto")
    void cupoDesdeServicio() {
        when(servicioRepositorio.findAll()).thenReturn(List.of(servicio(EstanciaService.SERVICIO_GUARDERIA, "35", 5)));
        assertEquals(5, servicio.cupoGuarderia());
        when(servicioRepositorio.findAll()).thenReturn(List.of(servicio(EstanciaService.SERVICIO_GUARDERIA, "35", null)));
        assertEquals(8, servicio.cupoGuarderia());
        when(servicioRepositorio.findAll()).thenReturn(List.of());
        assertEquals(8, servicio.cupoGuarderia());
    }

    @Test
    @DisplayName("Los días de guardería cuentan el primero y el último")
    void diasDeGuarderia() {
        assertEquals(1, EstanciaService.dias(manana.atTime(9, 0), manana.atTime(18, 0)));
        assertEquals(3, EstanciaService.dias(manana.atTime(9, 0), manana.plusDays(2).atTime(9, 0)));
    }

    @Test
    @DisplayName("Los cuidados especiales admiten hasta 500 caracteres y la modalidad debe ser válida")
    void validaciones() {
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.reservar(CLIENTE, "Guarderia", 7, manana.atTime(9, 0), manana.atTime(18, 0), "a".repeat(501), pago)).getEstado());
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.reservar(CLIENTE, "Spa", 7, manana.atTime(9, 0), manana.atTime(18, 0), null, pago)).getEstado());
        verifyNoInteractions(autorizacion);
    }

    @Test
    @DisplayName("El horario de ingresos y salidas sale de la configuración")
    void horarioConfigurable() {
        clienteConMascota();
        when(configuracion.aperturaHospedaje()).thenReturn(10);
        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.reservar(CLIENTE, "Guarderia", 7, manana.atTime(9, 0), manana.atTime(18, 0), null, pago));
        assertEquals(400, ex.getEstado());
        assertTrue(ex.getMessage().contains("10:00 a 20:00"));
        verify(estanciaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Solo se interna una mascota cuya cirugía está en postoperatorio")
    void internarSoloEnPostoperatorio() {
        Usuario vet = new Usuario();
        vet.setId(4);
        vet.setRol_id(3);
        when(autorizacion.conRol(4, 1, 3)).thenReturn(vet);
        CirugiaProgramada c = new CirugiaProgramada();
        c.setId(5);
        c.setVeterinario_id(4);
        c.setMascota_id(7);
        c.setEstado("En_curso");
        when(cirugiaRepositorio.findById(5)).thenReturn(Optional.of(c));
        assertEquals(409, assertThrows(NegocioException.class,
                () -> servicio.internar(4, 5, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null)).getEstado());
        verify(estanciaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Una reserva de hospedaje o guardería necesita un pago vigente para registrar el ingreso")
    void ingresoExigePago() {
        Usuario aux = new Usuario();
        aux.setId(50);
        aux.setRol_id(4);
        when(autorizacion.conRol(50, 1, 4, 3)).thenReturn(aux);
        Estancia e = guarderia(1, LocalDate.now());
        when(estanciaRepositorio.findById(1)).thenReturn(Optional.of(e));
        when(pagoRepositorio.findAll()).thenReturn(new ArrayList<>());
        assertEquals(409, assertThrows(NegocioException.class, () -> servicio.registrarIngreso(50, 1)).getEstado());
        verify(notificacionService, never()).notificar(anyInt(), anyString(), anyString(), anyString());
    }
}

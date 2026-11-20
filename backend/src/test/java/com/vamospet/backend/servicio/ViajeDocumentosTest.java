package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Servicio;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.modelo.ViajeDocumento;
import com.vamospet.backend.modelo.ViajeSolicitud;
import com.vamospet.backend.repositorio.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViajeDocumentosTest {

    private static final int CLIENTE = 2;
    private static final int AUX = 50;
    private static final byte[] PDF = "%PDF-1.4 documento".getBytes();

    @Mock private ViajeSolicitudRepository viajeRepositorio;
    @Mock private ViajeMascotaRepository viajeMascotaRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private PagoRepository pagoRepositorio;
    @Mock private ComprobanteRepository comprobanteRepositorio;
    @Mock private AutorizacionService autorizacion;
    @Mock private PagoService pagoService;
    @Mock private NotificacionService notificacionService;
    @Mock private ViajeDocumentoRepository documentoRepositorio;
    @InjectMocks private ViajeService servicio;

    private final List<String> creados = new ArrayList<>();

    @AfterEach
    void limpiar() throws IOException {
        for (String n : creados) Files.deleteIfExists(ViajeService.CARPETA.resolve(n));
    }

    private Usuario usuario(int id, int rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol_id(rol);
        return u;
    }

    private Map<String, ViajeService.Archivo> archivos(String... claves) {
        Map<String, ViajeService.Archivo> m = new LinkedHashMap<>();
        for (String k : claves) m.put(k, new ViajeService.Archivo(k + ".pdf", PDF));
        return m;
    }

    private ViajeSolicitud viaje(String estado) {
        ViajeSolicitud v = new ViajeSolicitud();
        v.setId(3);
        v.setUsuario_id(CLIENTE);
        v.setDestino("Cusco");
        v.setEstado(estado);
        return v;
    }

    private ViajeDocumento documento(int id, String requisito, String estado) {
        ViajeDocumento d = new ViajeDocumento();
        d.setId(id);
        d.setViaje_id(3);
        d.setRequisito(requisito);
        d.setEstado(estado);
        d.setVigente(true);
        d.setNombre_original(requisito + ".pdf");
        return d;
    }

    @Test
    @DisplayName("Los requisitos del viaje son certificado de salud, microchip y vacunas")
    void requisitos() {
        assertEquals(List.of("certificado_salud", "microchip", "vacunas"),
                servicio.requisitos().stream().map(r -> r.get("clave")).toList());
    }

    @Test
    @DisplayName("Si falta un documento la solicitud se rechaza y no se guarda nada")
    void faltaUnRequisito() {
        when(autorizacion.conRol(CLIENTE, 2)).thenReturn(usuario(CLIENTE, 2));
        NegocioException ex = assertThrows(NegocioException.class, () -> servicio.solicitar(CLIENTE, List.of(7),
                LocalDate.now().plusDays(10), "Cusco", archivos("certificado_salud", "vacunas"), null));
        assertEquals(400, ex.getEstado());
        assertTrue(ex.getMessage().contains("Constancia de microchip"));
        verifyNoInteractions(viajeRepositorio, documentoRepositorio);
    }

    @Test
    @DisplayName("Un documento que no es PDF ni JPEG se rechaza indicando qué requisito falló")
    void documentoInvalido() {
        when(autorizacion.conRol(CLIENTE, 2)).thenReturn(usuario(CLIENTE, 2));
        Mascota m = new Mascota();
        m.setId(7);
        m.setUsuario_id(CLIENTE);
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(m));
        Servicio base = new Servicio();
        base.setNombre(ViajeService.SERVICIO_BASE);
        base.setPrecio_base(new BigDecimal("150"));
        base.setActivo(true);
        when(servicioRepositorio.findAll()).thenReturn(List.of(base));
        Map<String, ViajeService.Archivo> docs = archivos("certificado_salud", "microchip");
        docs.put("vacunas", new ViajeService.Archivo("vacunas.pdf", "hola, no soy un pdf".getBytes()));
        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.solicitar(CLIENTE, List.of(7), LocalDate.now().plusDays(10), "Cusco", docs, null));
        assertEquals(400, ex.getEstado());
        assertTrue(ex.getMessage().startsWith("Carné de vacunas"));
        verifyNoInteractions(viajeRepositorio, documentoRepositorio, pagoService);
    }

    @Test
    @DisplayName("Rechazar exige marcar qué documento está mal y deja los demás aceptados")
    void rechazarMarcaObservados() {
        when(autorizacion.conRol(AUX, 1, 3, 4)).thenReturn(usuario(AUX, 4));
        when(viajeRepositorio.findById(3)).thenReturn(Optional.of(viaje("Registrada")));
        ViajeDocumento salud = documento(11, "certificado_salud", "Pendiente");
        ViajeDocumento chip = documento(12, "microchip", "Pendiente");
        ViajeDocumento vacunas = documento(13, "vacunas", "Pendiente");
        when(documentoRepositorio.deViaje(3)).thenReturn(List.of(salud, chip, vacunas));
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.revisar(AUX, 3, "Rechazada", "Ilegible", List.of())).getEstado());
        when(viajeRepositorio.save(any(ViajeSolicitud.class))).thenAnswer(i -> i.getArgument(0));
        ViajeSolicitud r = servicio.revisar(AUX, 3, "Rechazada", "El carné está borroso", List.of("vacunas"));
        assertEquals("Rechazada", r.getEstado());
        assertEquals("Aceptado", salud.getEstado());
        assertEquals("Aceptado", chip.getEstado());
        assertEquals("Observado", vacunas.getEstado());
    }

    @Test
    @DisplayName("No se aprueba un viaje marcando documentos observados")
    void aprobarSinObservados() {
        when(autorizacion.conRol(AUX, 1, 3, 4)).thenReturn(usuario(AUX, 4));
        when(viajeRepositorio.findById(3)).thenReturn(Optional.of(viaje("Registrada")));
        when(documentoRepositorio.deViaje(3)).thenReturn(List.of());
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.revisar(AUX, 3, "Aprobada", null, List.of("microchip"))).getEstado());
        verify(viajeRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Al reenviar solo se aceptan los documentos observados")
    void reenviarSoloObservados() {
        when(autorizacion.conRol(CLIENTE, 2)).thenReturn(usuario(CLIENTE, 2));
        when(viajeRepositorio.findById(3)).thenReturn(Optional.of(viaje("Rechazada")));
        when(documentoRepositorio.deViaje(3)).thenReturn(List.of(documento(11, "certificado_salud", "Aceptado"),
                documento(12, "microchip", "Aceptado"), documento(13, "vacunas", "Observado")));
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.reenviarDocumentos(CLIENTE, 3, archivos("microchip"))).getEstado());
        verify(documentoRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Reenviar el documento observado reemplaza el anterior y la solicitud vuelve a revisión")
    void reenviarVuelveARevision() {
        when(autorizacion.conRol(CLIENTE, 2)).thenReturn(usuario(CLIENTE, 2));
        ViajeSolicitud v = viaje("Rechazada");
        when(viajeRepositorio.findById(3)).thenReturn(Optional.of(v));
        when(viajeRepositorio.save(any(ViajeSolicitud.class))).thenAnswer(i -> i.getArgument(0));
        ViajeDocumento observado = documento(13, "vacunas", "Observado");
        when(documentoRepositorio.deViaje(3)).thenReturn(List.of(documento(11, "certificado_salud", "Aceptado"),
                documento(12, "microchip", "Aceptado"), observado));
        when(documentoRepositorio.save(any(ViajeDocumento.class))).thenAnswer(i -> {
            ViajeDocumento d = i.getArgument(0);
            if (d.getNombre_almacenado() != null) creados.add(d.getNombre_almacenado());
            return d;
        });
        servicio.reenviarDocumentos(CLIENTE, 3, archivos("vacunas"));
        assertEquals("Registrada", v.getEstado());
        assertFalse(observado.getVigente());
        verify(documentoRepositorio).save(argThat(d -> "vacunas".equals(d.getRequisito()) && Boolean.TRUE.equals(d.getVigente())
                && "Pendiente".equals(d.getEstado())));
        assertEquals(1, creados.size());
    }

    @Test
    @DisplayName("Un cliente no reenvía documentos de un viaje ajeno")
    void reenviarAjeno() {
        when(autorizacion.conRol(8, 2)).thenReturn(usuario(8, 2));
        when(viajeRepositorio.findById(3)).thenReturn(Optional.of(viaje("Rechazada")));
        assertEquals(403, assertThrows(NegocioException.class,
                () -> servicio.reenviarDocumentos(8, 3, archivos("vacunas"))).getEstado());
        verifyNoInteractions(documentoRepositorio);
    }
}

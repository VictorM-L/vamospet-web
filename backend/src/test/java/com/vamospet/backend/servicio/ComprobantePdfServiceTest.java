package com.vamospet.backend.servicio;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprobantePdfServiceTest {

    @Mock private PagoRepository pagoRepositorio;
    @Mock private UsuarioRepository usuarioRepositorio;
    @Mock private PedidoDetalleRepository pedidoDetalleRepositorio;
    @Mock private ProductoRepository productoRepositorio;
    @Mock private CitaRepository citaRepositorio;
    @Mock private ServicioRepository servicioRepositorio;
    @Mock private MascotaRepository mascotaRepositorio;
    @Mock private CitaCostoAdicionalRepository costoRepositorio;
    @Mock private EstanciaRepository estanciaRepositorio;
    @Mock private CuartoRepository cuartoRepositorio;
    @Mock private ViajeSolicitudRepository viajeRepositorio;
    @Mock private ViajeMascotaRepository viajeMascotaRepositorio;
    @Mock private ConfiguracionService configuracion;
    @InjectMocks private ComprobantePdfService servicio;

    private static String texto(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        for (int i = 1; i <= lector.getNumberOfPages(); i++) sb.append(extractor.getTextFromPage(i)).append('\n');
        lector.close();
        return sb.toString();
    }

    @Test
    @DisplayName("El PDF trae los datos de la clínica, el cliente, el detalle y el total; la guardería sin cuarto no falla")
    void pdfConDatosCorrectos() throws Exception {
        ConfiguracionClinica clinica = new ConfiguracionClinica();
        clinica.setNombre("Vamos Pet Miraflores");
        clinica.setDireccion("Calle San Martin 849");
        clinica.setTelefono("+51 987 654 321");
        clinica.setCorreo("hola@vamospet.pe");
        when(configuracion.actual()).thenReturn(clinica);
        Pago pago = new Pago();
        pago.setId(40);
        pago.setUsuario_id(2);
        pago.setEstancia_id(9);
        pago.setMonto(new BigDecimal("70.00"));
        pago.setMetodo(PagoService.TARJETA);
        pago.setTarjeta_ultimos4("4242");
        pago.setFecha(LocalDateTime.of(2026, 11, 18, 10, 0));
        when(pagoRepositorio.findById(40)).thenReturn(Optional.of(pago));
        Usuario cliente = new Usuario();
        cliente.setNombre("Ana Torres");
        cliente.setDni("71234567");
        when(usuarioRepositorio.findById(2)).thenReturn(Optional.of(cliente));
        Estancia e = new Estancia();
        e.setId(9);
        e.setMascota_id(7);
        e.setTipo(EstanciaService.GUARDERIA);
        e.setIngreso(LocalDateTime.of(2026, 12, 1, 9, 0));
        e.setSalida_estimada(LocalDateTime.of(2026, 12, 2, 18, 0));
        e.setPrecio_unitario(new BigDecimal("35.00"));
        when(estanciaRepositorio.findById(9)).thenReturn(Optional.of(e));
        Mascota m = new Mascota();
        m.setNombre("Toby");
        when(mascotaRepositorio.findById(7)).thenReturn(Optional.of(m));
        Comprobante c = new Comprobante();
        c.setPago_id(40);
        c.setTipo("Boleta");
        c.setNumero("B001-E9");
        c.setCodigo_verificacion("abc-123");

        byte[] pdf = servicio.generar(c);
        assertTrue(pdf.length > 0 && new String(pdf, 0, 5).equals("%PDF-"));
        String t = texto(pdf);
        assertTrue(t.contains("Vamos Pet Miraflores"), t);
        assertTrue(t.contains("Tel. +51 987 654 321"), t);
        assertTrue(t.contains("B001-E9"), t);
        assertTrue(t.contains("Ana Torres"), t);
        assertTrue(t.contains("4242"), t);
        assertTrue(t.contains("Toby"), t);
        assertTrue(t.contains("S/ 70.00"), t);
        verify(cuartoRepositorio, never()).findById(any());
    }

    @Test
    @DisplayName("Los datos de la clínica se arman sin dejar separadores vacíos")
    void datosClinica() {
        ConfiguracionClinica c = new ConfiguracionClinica();
        assertEquals("Clínica Veterinaria", ComprobantePdfService.datosClinica(c));
        c.setTelefono("+51 900 000 001");
        assertEquals("Tel. +51 900 000 001", ComprobantePdfService.datosClinica(c));
        c.setDireccion("Calle 1");
        assertEquals("Calle 1  ·  Tel. +51 900 000 001", ComprobantePdfService.datosClinica(c));
    }
}

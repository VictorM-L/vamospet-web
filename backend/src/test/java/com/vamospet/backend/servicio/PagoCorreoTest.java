package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Comprobante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoCorreoTest {

    @Mock private NotificacionService notificacionService;
    @Mock private ComprobantePdfService comprobantePdfService;
    @InjectMocks private PagoService servicio;

    private Comprobante comprobante() {
        Comprobante c = new Comprobante();
        c.setNumero("B001-77");
        return c;
    }

    @Test
    @DisplayName("El pago con tarjeta envía el comprobante en PDF adjunto")
    void adjuntaPdf() throws Exception {
        Comprobante c = comprobante();
        byte[] pdf = "%PDF-1.4 prueba".getBytes();
        when(comprobantePdfService.generar(c)).thenReturn(pdf);
        servicio.enviarComprobantePorCorreo("ana@correo.pe", c, "Gracias por tu pago.");
        verify(notificacionService).enviarCorreo(eq("ana@correo.pe"), eq("Vamos Pet — Comprobante B001-77"),
                argThat(t -> t.contains("Gracias por tu pago.") && t.contains("PDF")), eq("comprobante-B001-77.pdf"), eq(pdf));
        verify(notificacionService, never()).enviarCorreoDirecto(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Si el PDF no se puede generar, igual llega el correo sin adjunto")
    void sinPdfIgualAvisa() throws Exception {
        Comprobante c = comprobante();
        when(comprobantePdfService.generar(c)).thenThrow(new IllegalStateException("fallo"));
        servicio.enviarComprobantePorCorreo("ana@correo.pe", c, "Gracias por tu pago.");
        verify(notificacionService).enviarCorreoDirecto("ana@correo.pe", "Comprobante B001-77", "Gracias por tu pago.");
    }
}

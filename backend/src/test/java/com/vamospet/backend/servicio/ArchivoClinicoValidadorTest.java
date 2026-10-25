package com.vamospet.backend.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ArchivoClinicoValidadorTest {

    private static byte[] pdf(String extra) {
        return ("%PDF-1.4\n" + extra + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    }

    @Test
    @DisplayName("Un PDF real con extensión .pdf es válido")
    void pdfValido() {
        ArchivoClinicoValidador.Tipo t = ArchivoClinicoValidador.validar("resultado.PDF", pdf("1 0 obj"));
        assertEquals("pdf", t.extension());
        assertEquals("application/pdf", t.contentType());
    }

    @Test
    @DisplayName("Un JPEG real con .jpg o .jpeg es válido")
    void jpegValido() {
        assertEquals("image/jpeg", ArchivoClinicoValidador.validar("rx.jpg", jpeg()).contentType());
        assertEquals("image/jpeg", ArchivoClinicoValidador.validar("rx.jpeg", jpeg()).contentType());
    }

    @Test
    @DisplayName("Un ejecutable renombrado a .pdf se rechaza por su contenido")
    void contenidoFalso() {
        byte[] exe = "MZ\u0090\u0000\u0003".getBytes(StandardCharsets.ISO_8859_1);
        NegocioException ex = assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar("virus.pdf", exe));
        assertEquals(400, ex.getEstado());
    }

    @Test
    @DisplayName("Un JPEG con extensión .pdf (o al revés) se rechaza")
    void extensionNoCoincide() {
        assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar("foto.pdf", jpeg()));
        assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar("doc.jpg", pdf("x")));
    }

    @Test
    @DisplayName("Solo PDF o JPEG: png, gif, exe, sin extensión se rechazan")
    void extensionesNoPermitidas() {
        for (String nombre : new String[]{"a.png", "a.gif", "a.exe", "a.html", "sinextension"}) {
            assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar(nombre, jpeg()), nombre);
        }
    }

    @Test
    @DisplayName("Máximo 5 MB y no vacío")
    void tamano() {
        assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar("a.pdf", new byte[0]));
        byte[] grande = new byte[ArchivoClinicoValidador.TAMANO_MAXIMO_BYTES + 1];
        System.arraycopy(pdf("x"), 0, grande, 0, 6);
        assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar("a.pdf", grande));
    }

    @Test
    @DisplayName("Un PDF con JavaScript se rechaza")
    void pdfConScript() {
        assertThrows(NegocioException.class, () -> ArchivoClinicoValidador.validar("a.pdf", pdf("/S /JavaScript /JS (app.alert(1))")));
    }
}

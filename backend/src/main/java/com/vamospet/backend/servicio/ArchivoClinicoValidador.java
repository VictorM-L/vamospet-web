package com.vamospet.backend.servicio;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

public final class ArchivoClinicoValidador {

    public static final int TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("pdf", "jpg", "jpeg");

    public record Tipo(String extension, String contentType) {}

    private ArchivoClinicoValidador() {}

    public static Tipo validar(String nombreOriginal, byte[] contenido) {
        if (contenido == null || contenido.length == 0) throw new NegocioException(400, "El archivo está vacío.");
        if (contenido.length > TAMANO_MAXIMO_BYTES) throw new NegocioException(400, "El archivo supera los 5 MB permitidos.");

        String nombre = nombreOriginal == null ? "" : nombreOriginal;
        int punto = nombre.lastIndexOf('.');
        String ext = punto >= 0 ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONES.contains(ext)) {
            throw new NegocioException(400, "Formato no permitido. Solo se aceptan archivos PDF o JPEG (.pdf, .jpg, .jpeg).");
        }

        boolean esPdf = empiezaCon(contenido, new byte[]{'%', 'P', 'D', 'F', '-'});
        boolean esJpeg = empiezaCon(contenido, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        if (!esPdf && !esJpeg) {
            throw new NegocioException(400, "El contenido del archivo no es un PDF ni un JPEG válido.");
        }
        if (esPdf != ext.equals("pdf")) {
            throw new NegocioException(400, "La extensión del archivo no coincide con su contenido real.");
        }
        if (esPdf && contieneScript(contenido)) {
            throw new NegocioException(400, "El PDF contiene código activo (JavaScript) y no se acepta.");
        }
        return esPdf ? new Tipo("pdf", "application/pdf") : new Tipo("jpg", "image/jpeg");
    }

    private static boolean empiezaCon(byte[] datos, byte[] firma) {
        if (datos.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) if (datos[i] != firma[i]) return false;
        return true;
    }

    private static boolean contieneScript(byte[] datos) {
        String texto = new String(datos, StandardCharsets.ISO_8859_1);
        return texto.contains("/JavaScript") || texto.contains("/JS ") || texto.contains("/JS(") || texto.contains("/Launch");
    }
}

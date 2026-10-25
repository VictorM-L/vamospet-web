package com.vamospet.backend.servicio;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ReportePdfService {

    private static final Color VERDE = new Color(22, 120, 80);
    private static final Color GRIS = new Color(107, 114, 128);

    public byte[] generar(ReporteService.Tabla t) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Rectangle pagina = t.columnas.size() > 6 ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(pagina, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, salida);
        doc.open();

        Paragraph marca = new Paragraph("Vamos Pet — Clínica Veterinaria", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, VERDE));
        doc.add(marca);
        Paragraph titulo = new Paragraph(t.titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        titulo.setSpacingBefore(4);
        doc.add(titulo);
        doc.add(new Paragraph(t.subtitulo, FontFactory.getFont(FontFactory.HELVETICA, 10, GRIS)));
        Paragraph gen = new Paragraph("Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS));
        gen.setSpacingAfter(12);
        doc.add(gen);

        if (!t.resumen.isEmpty()) {
            PdfPTable res = new PdfPTable(2);
            res.setWidthPercentage(60);
            res.setHorizontalAlignment(Element.ALIGN_LEFT);
            res.setSpacingAfter(14);
            for (var par : t.resumen) {
                res.addCell(celda(par.get(0), FontFactory.getFont(FontFactory.HELVETICA, 10), Element.ALIGN_LEFT, null));
                res.addCell(celda(par.get(1), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10), Element.ALIGN_RIGHT, null));
            }
            doc.add(res);
        }

        PdfPTable tabla = new PdfPTable(t.columnas.size());
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        Font cab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String c : t.columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(c, cab));
            cell.setBackgroundColor(VERDE);
            cell.setPadding(5);
            tabla.addCell(cell);
        }
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        if (t.filas.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("No hay datos para el periodo seleccionado.", normal));
            vacio.setColspan(t.columnas.size());
            vacio.setPadding(10);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(vacio);
        }
        for (int i = 0; i < t.filas.size(); i++) {
            var fila = t.filas.get(i);
            String color = i < t.colores.size() ? t.colores.get(i) : null;
            for (int j = 0; j < fila.size(); j++) {
                boolean ultima = j == fila.size() - 1;
                tabla.addCell(celda(fila.get(j), normal, Element.ALIGN_LEFT, ultima ? colorDe(color) : null));
            }
        }
        doc.add(tabla);
        doc.close();
        return salida.toByteArray();
    }

    private Color colorDe(String c) {
        if (c == null) return null;
        return switch (c) {
            case "rojo" -> new Color(254, 202, 202);
            case "amarillo" -> new Color(253, 230, 138);
            case "verde" -> new Color(187, 247, 208);
            default -> null;
        };
    }

    private PdfPCell celda(String texto, Font f, int alineacion, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setHorizontalAlignment(alineacion);
        c.setPadding(4);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new Color(229, 231, 235));
        if (fondo != null) c.setBackgroundColor(fondo);
        return c;
    }
}

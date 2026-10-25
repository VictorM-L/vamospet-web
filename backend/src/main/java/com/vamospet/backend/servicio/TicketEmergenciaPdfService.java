package com.vamospet.backend.servicio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.vamospet.backend.modelo.ContactoEmergencia;
import com.vamospet.backend.modelo.Mascota;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

@Service
public class TicketEmergenciaPdfService {

    private static final Color VERDE = new Color(22, 120, 80);
    private static final Color GRIS = new Color(107, 114, 128);

    public byte[] generar(String codigo, Mascota m, ContactoEmergencia c) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A6, 24, 24, 24, 24);
        PdfWriter writer = PdfWriter.getInstance(doc, salida);
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, VERDE);
        Font codigoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font chico = FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS);

        Paragraph p = new Paragraph("Vamos Pet", titulo);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
        Paragraph sub = new Paragraph("INGRESO POR EMERGENCIA", negrita);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(8);
        doc.add(sub);

        Paragraph cod = new Paragraph(codigo, codigoFont);
        cod.setAlignment(Element.ALIGN_CENTER);
        cod.setSpacingAfter(6);
        doc.add(cod);

        Image qr = crearQr(writer, codigo, 90);
        qr.setAlignment(Element.ALIGN_CENTER);
        doc.add(qr);

        doc.add(new Paragraph(" ", chico));
        doc.add(new Paragraph("Mascota: " + m.getNombre() + " (" + m.getEspecie() + ")", normal));
        if (c != null) {
            doc.add(new Paragraph("Acompañante: " + c.getNombre_acompanante(), normal));
            doc.add(new Paragraph("Teléfono: " + c.getTelefono(), normal));
            if (c.getFecha_ingreso() != null) {
                doc.add(new Paragraph("Ingreso: " + c.getFecha_ingreso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normal));
            }
        }
        Paragraph pie = new Paragraph("Conserva este ticket. Cuando el dueño tenga su cuenta en Vamos Pet, podrá vincular a la mascota "
                + "desde su perfil con este código y conservará todo el historial de la atención.", chico);
        pie.setSpacingBefore(10);
        doc.add(pie);

        doc.close();
        return salida.toByteArray();
    }

    private Image crearQr(PdfWriter writer, String contenido, float tamano) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0, hints);
        int n = matriz.getWidth();
        float modulo = tamano / n;
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate plantilla = cb.createTemplate(tamano, tamano);
        plantilla.setColorFill(Color.BLACK);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (matriz.get(x, y)) plantilla.rectangle(x * modulo, tamano - (y + 1) * modulo, modulo, modulo);
            }
        }
        plantilla.fill();
        return Image.getInstance(plantilla);
    }
}

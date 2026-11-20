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
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ComprobantePdfService {

    @Value("${app.frontend.url:http://localhost:8080/index.html}")
    private String frontendUrl;

    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepositorio;
    @Autowired private ProductoRepository productoRepositorio;
    @Autowired private CitaRepository citaRepositorio;
    @Autowired private ServicioRepository servicioRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private CitaCostoAdicionalRepository costoRepositorio;
    @Autowired private EstanciaRepository estanciaRepositorio;
    @Autowired private CuartoRepository cuartoRepositorio;
    @Autowired private ViajeSolicitudRepository viajeRepositorio;
    @Autowired private ViajeMascotaRepository viajeMascotaRepositorio;
    @Autowired private ConfiguracionService configuracion;

    private static final Color VERDE = new Color(22, 120, 80);
    private static final Color GRIS = new Color(107, 114, 128);

    public String urlVerificacion(Comprobante c) {
        return frontendUrl + "?verificar=" + c.getCodigo_verificacion();
    }

    public byte[] generar(Comprobante c) throws Exception {
        Pago pago = c.getPago_id() == null ? null : pagoRepositorio.findById(c.getPago_id()).orElse(null);
        Usuario cliente = pago == null || pago.getUsuario_id() == null ? null : usuarioRepositorio.findById(pago.getUsuario_id()).orElse(null);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, salida);
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VERDE);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font chico = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);

        ConfiguracionClinica clinica = configuracion.actual();
        Paragraph p = new Paragraph(vacio(clinica.getNombre()) ? "Vamos Pet Clínica Veterinaria" : clinica.getNombre(), titulo);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
        Paragraph sub = new Paragraph(datosClinica(clinica), chico);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(10);
        doc.add(sub);

        Paragraph tipo = new Paragraph((c.getTipo() == null ? "Boleta" : c.getTipo()).toUpperCase() + " DE VENTA  N° " + c.getNumero(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        tipo.setAlignment(Element.ALIGN_CENTER);
        tipo.setSpacingAfter(10);
        doc.add(tipo);

        String fecha = pago != null && pago.getFecha() != null
                ? pago.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        doc.add(new Paragraph("Fecha: " + fecha, normal));
        doc.add(new Paragraph("Cliente: " + (cliente != null && cliente.getNombre() != null ? cliente.getNombre() : "—"), normal));
        if (cliente != null && cliente.getDni() != null && cliente.getDni().length() > 2) {
            doc.add(new Paragraph("DNI: " + cliente.getDni().substring(0, 2) + "*".repeat(cliente.getDni().length() - 2), normal));
        }
        doc.add(new Paragraph("Método de pago: " + descripcionMetodo(pago), normal));

        PdfPTable tabla = new PdfPTable(new float[]{5f, 1.3f, 2f, 2f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(12);
        for (String h : new String[]{"Descripción", "Cant.", "P. unit.", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            cell.setBackgroundColor(VERDE);
            cell.setPadding(5);
            tabla.addCell(cell);
        }

        BigDecimal suma = BigDecimal.ZERO;
        if (pago != null && pago.getPedido_id() != null) {
            List<PedidoDetalle> detalle = pedidoDetalleRepositorio.findAll().stream()
                    .filter(d -> pago.getPedido_id().equals(d.getPedido_id())).toList();
            for (PedidoDetalle d : detalle) {
                Producto prod = d.getProducto_id() == null ? null : productoRepositorio.findById(d.getProducto_id()).orElse(null);
                BigDecimal sub1 = d.getPrecio_unitario().multiply(BigDecimal.valueOf(d.getCantidad()));
                fila(tabla, prod != null ? prod.getNombre() : "Producto #" + d.getProducto_id(), String.valueOf(d.getCantidad()),
                        d.getPrecio_unitario(), sub1, normal);
                suma = suma.add(sub1);
            }
        } else if (pago != null && pago.getCita_id() != null) {
            Cita cita = citaRepositorio.findById(pago.getCita_id()).orElse(null);
            if (cita != null) {
                Servicio serv = cita.getServicio_id() == null ? null : servicioRepositorio.findById(cita.getServicio_id()).orElse(null);
                Mascota m = cita.getMascota_id() == null ? null : mascotaRepositorio.findById(cita.getMascota_id()).orElse(null);
                BigDecimal base = cita.getCosto() != null ? cita.getCosto() : (serv != null && serv.getPrecio_base() != null ? serv.getPrecio_base() : BigDecimal.ZERO);
                fila(tabla, (serv != null ? serv.getNombre() : "Servicio") + (m != null ? " — " + m.getNombre() : ""), "1", base, base, normal);
                suma = suma.add(base);
                for (CitaCostoAdicional x : costoRepositorio.findAll()) {
                    if (!cita.getId().equals(x.getCita_id())) continue;
                    fila(tabla, x.getConcepto(), "1", x.getMonto(), x.getMonto(), normal);
                    suma = suma.add(x.getMonto());
                }
            }
        } else if (pago != null && pago.getEstancia_id() != null) {
            Estancia e = estanciaRepositorio.findById(pago.getEstancia_id()).orElse(null);
            if (e != null) {
                Mascota m = mascotaRepositorio.findById(e.getMascota_id()).orElse(null);
                Cuarto cuarto = e.getCuarto_id() == null ? null : cuartoRepositorio.findById(e.getCuarto_id()).orElse(null);
                java.time.LocalDateTime hasta = e.getSalida_real() != null ? e.getSalida_real() : e.getSalida_estimada();
                boolean guarderia = EstanciaService.GUARDERIA.equals(e.getTipo());
                long unidades = guarderia ? EstanciaService.dias(e.getIngreso(), e.getSalida_estimada())
                        : Math.max(1, (long) Math.ceil(java.time.Duration.between(e.getIngreso(), hasta).toMinutes() / 1440.0));
                String unidad = "Hospedaje".equals(e.getTipo()) ? "noche(s)" : "día(s)";
                fila(tabla, (guarderia ? "Guardería" : e.getTipo()) + (m != null ? " — " + m.getNombre() : "") + (cuarto != null ? " (" + cuarto.getNombre() + ")" : ""),
                        String.valueOf(unidades), e.getPrecio_unitario(), e.getPrecio_unitario().multiply(BigDecimal.valueOf(unidades)), normal);
                suma = suma.add(e.getPrecio_unitario().multiply(BigDecimal.valueOf(unidades)));
                DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                doc.add(new Paragraph("Ingreso: " + e.getIngreso().format(f) + "   Salida " + (e.getSalida_real() != null ? "real: " : "prevista: ")
                        + hasta.format(f) + "   (" + unidades + " " + unidad + ")", chico));
            }
        } else if (pago != null && pago.getViaje_id() != null) {
            ViajeSolicitud v = viajeRepositorio.findById(pago.getViaje_id()).orElse(null);
            if (v != null) {
                BigDecimal base = precioServicio(ViajeService.SERVICIO_BASE);
                BigDecimal adicional = precioServicio(ViajeService.SERVICIO_ADICIONAL);
                List<ViajeMascota> mascotas = viajeMascotaRepositorio.findAll().stream().filter(x -> v.getId().equals(x.getViaje_id())).toList();
                int i = 0;
                for (ViajeMascota vm : mascotas) {
                    Mascota m = mascotaRepositorio.findById(vm.getMascota_id()).orElse(null);
                    BigDecimal precio = i == 0 ? base : adicional;
                    fila(tabla, (i == 0 ? "Viaja con tu mascota — " : "Mascota adicional — ") + (m != null ? m.getNombre() : "#" + vm.getMascota_id()), "1", precio, precio, normal);
                    suma = suma.add(precio);
                    i++;
                }
                doc.add(new Paragraph("Fecha de viaje: " + v.getFecha_viaje().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + (v.getDestino() != null ? "   Destino: " + v.getDestino() : ""), chico));
            }
        }
        doc.add(tabla);

        BigDecimal total = pago != null && pago.getMonto() != null ? pago.getMonto() : suma;
        Paragraph tot = new Paragraph("TOTAL: S/ " + total.setScale(2, java.math.RoundingMode.HALF_UP), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
        tot.setAlignment(Element.ALIGN_RIGHT);
        tot.setSpacingBefore(8);
        tot.setSpacingAfter(12);
        doc.add(tot);

        Image qr = crearQr(writer, urlVerificacion(c), 110);
        qr.setAlignment(Element.ALIGN_CENTER);
        doc.add(qr);
        Paragraph v = new Paragraph("Escanea el código para verificar este comprobante", chico);
        v.setAlignment(Element.ALIGN_CENTER);
        doc.add(v);
        Paragraph aviso = new Paragraph("Comprobante interno de Vamos Pet. No constituye comprobante de pago electrónico SUNAT.", chico);
        aviso.setAlignment(Element.ALIGN_CENTER);
        aviso.setSpacingBefore(6);
        doc.add(aviso);

        doc.close();
        return salida.toByteArray();
    }

    static String datosClinica(ConfiguracionClinica c) {
        StringBuilder sb = new StringBuilder();
        for (String parte : new String[]{c.getDireccion(), c.getTelefono() == null || c.getTelefono().isBlank() ? null : "Tel. " + c.getTelefono(), c.getCorreo()}) {
            if (vacio(parte)) continue;
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(parte.trim());
        }
        return sb.length() == 0 ? "Clínica Veterinaria" : sb.toString();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private BigDecimal precioServicio(String nombre) {
        return servicioRepositorio.findAll().stream()
                .filter(x -> x.getNombre() != null && x.getNombre().equalsIgnoreCase(nombre) && x.getPrecio_base() != null)
                .map(Servicio::getPrecio_base).findFirst().orElse(BigDecimal.ZERO);
    }

    private String descripcionMetodo(Pago pago) {
        if (pago == null || pago.getMetodo() == null) return "—";
        if (PagoService.TARJETA.equals(pago.getMetodo()) && pago.getTarjeta_ultimos4() != null) return "Tarjeta terminada en " + pago.getTarjeta_ultimos4();
        if (PagoService.YAPE.equals(pago.getMetodo()) && pago.getOperacion_yape() != null) return "Yape (operación " + pago.getOperacion_yape() + ")";
        return pago.getMetodo();
    }

    private void fila(PdfPTable t, String desc, String cant, BigDecimal pu, BigDecimal sub, Font f) {
        t.addCell(celda(desc, f, Element.ALIGN_LEFT));
        t.addCell(celda(cant, f, Element.ALIGN_CENTER));
        t.addCell(celda("S/ " + pu.setScale(2, java.math.RoundingMode.HALF_UP), f, Element.ALIGN_RIGHT));
        t.addCell(celda("S/ " + sub.setScale(2, java.math.RoundingMode.HALF_UP), f, Element.ALIGN_RIGHT));
    }

    private PdfPCell celda(String texto, Font f, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setHorizontalAlignment(alineacion);
        c.setPadding(4);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new Color(229, 231, 235));
        return c;
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

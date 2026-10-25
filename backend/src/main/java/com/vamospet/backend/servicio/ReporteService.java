package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReporteService {

    public static class Tabla {
        public String titulo;
        public String subtitulo;
        public List<String> columnas = new ArrayList<>();
        public List<List<String>> filas = new ArrayList<>();
        public List<String> colores = new ArrayList<>();
        public List<List<String>> resumen = new ArrayList<>();

        void fila(String... celdas) { filas.add(Arrays.asList(celdas)); colores.add(null); }
        void filaColor(String color, String... celdas) { filas.add(Arrays.asList(celdas)); colores.add(color); }
        void dato(String etiqueta, String valor) { resumen.add(List.of(etiqueta, valor)); }
    }

    public static final ImmutableList<String> TIPOS =
            ImmutableList.of("ingresos", "veterinarios", "asistencia", "inventario", "comprobantes");

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private DevolucionRepository devolucionRepositorio;
    @Autowired private CitaRepository citaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private ProductoRepository productoRepositorio;
    @Autowired private LoteProductoRepository loteRepositorio;
    @Autowired private ComprobanteRepository comprobanteRepositorio;

    public Tabla generar(String tipo, LocalDate desde, LocalDate hasta) {
        return switch (tipo) {
            case "ingresos" -> ingresos(desde, hasta);
            case "veterinarios" -> veterinarios(desde, hasta);
            case "asistencia" -> asistencia(desde, hasta);
            case "inventario" -> inventario();
            case "comprobantes" -> comprobantes(desde, hasta);
            default -> null;
        };
    }

    private String rango(LocalDate d, LocalDate h) { return "Del " + d.format(DIA) + " al " + h.format(DIA); }
    private boolean enRango(LocalDate f, LocalDate d, LocalDate h) { return f != null && !f.isBefore(d) && !f.isAfter(h); }
    private String soles(BigDecimal v) { return "S/ " + (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP); }

    private Tabla ingresos(LocalDate d, LocalDate h) {
        Tabla t = new Tabla();
        t.titulo = "Ingresos por periodo";
        t.subtitulo = rango(d, h);
        t.columnas = List.of("Fecha", "N° de pagos", "Citas", "Pedidos", "Total");

        Map<LocalDate, BigDecimal[]> porDia = new TreeMap<>();
        Map<LocalDate, Integer> cuenta = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        int n = 0;
        for (Pago p : pagoRepositorio.findAll()) {
            if (p.getFecha() == null || p.getMonto() == null || !enRango(p.getFecha().toLocalDate(), d, h)) continue;
            LocalDate dia = p.getFecha().toLocalDate();
            BigDecimal[] acc = porDia.computeIfAbsent(dia, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (p.getCita_id() != null) acc[0] = acc[0].add(p.getMonto()); else acc[1] = acc[1].add(p.getMonto());
            cuenta.merge(dia, 1, Integer::sum);
            total = total.add(p.getMonto());
            n++;
        }
        for (var e : porDia.entrySet()) {
            t.fila(e.getKey().format(DIA), String.valueOf(cuenta.get(e.getKey())), soles(e.getValue()[0]), soles(e.getValue()[1]),
                    soles(e.getValue()[0].add(e.getValue()[1])));
        }
        BigDecimal reembolsos = BigDecimal.ZERO;
        for (Devolucion dv : devolucionRepositorio.findAll()) {
            if (dv.getFecha() != null && dv.getTotal_reembolso() != null && enRango(dv.getFecha().toLocalDate(), d, h)) {
                reembolsos = reembolsos.add(dv.getTotal_reembolso());
            }
        }
        t.dato("Total de pagos", String.valueOf(n));
        t.dato("Ingresos brutos", soles(total));
        t.dato("Reembolsos por devoluciones", soles(reembolsos));
        t.dato("Ingreso neto", soles(total.subtract(reembolsos)));
        return t;
    }

    private Tabla veterinarios(LocalDate d, LocalDate h) {
        Tabla t = new Tabla();
        t.titulo = "Citas y atenciones por veterinario";
        t.subtitulo = rango(d, h);
        t.columnas = List.of("Veterinario", "Total de citas", "Atendidas", "Pendientes / Confirmadas", "Canceladas", "No asistió");

        Map<Integer, String> nombres = new HashMap<>();
        for (Usuario u : usuarioRepositorio.findAll()) nombres.put(u.getId(), u.getNombre());
        Map<String, int[]> porVet = new TreeMap<>();
        for (Cita c : citaRepositorio.findAll()) {
            if (c.getFecha_hora() == null || !enRango(c.getFecha_hora().toLocalDate(), d, h)) continue;
            String vet = c.getVeterinario_id() == null ? "Sin asignar" : nombres.getOrDefault(c.getVeterinario_id(), "Vet #" + c.getVeterinario_id());
            int[] a = porVet.computeIfAbsent(vet, k -> new int[5]);
            a[0]++;
            switch (c.getEstado() == null ? "" : c.getEstado()) {
                case "Atendida" -> a[1]++;
                case "Cancelada" -> a[3]++;
                case "No asistió" -> a[4]++;
                default -> a[2]++;
            }
        }
        int[] suma = new int[5];
        for (var e : porVet.entrySet()) {
            int[] a = e.getValue();
            t.fila(e.getKey(), String.valueOf(a[0]), String.valueOf(a[1]), String.valueOf(a[2]), String.valueOf(a[3]), String.valueOf(a[4]));
            for (int i = 0; i < 5; i++) suma[i] += a[i];
        }
        t.dato("Total de citas", String.valueOf(suma[0]));
        t.dato("Atendidas", String.valueOf(suma[1]));
        return t;
    }

    private Tabla asistencia(LocalDate d, LocalDate h) {
        Tabla t = new Tabla();
        t.titulo = "Asistencia e inasistencia a citas";
        t.subtitulo = rango(d, h);
        t.columnas = List.of("Fecha", "Atendidas", "No asistió", "Canceladas", "% de asistencia");

        Map<LocalDate, int[]> porDia = new TreeMap<>();
        for (Cita c : citaRepositorio.findAll()) {
            if (c.getFecha_hora() == null || !enRango(c.getFecha_hora().toLocalDate(), d, h)) continue;
            int[] a = porDia.computeIfAbsent(c.getFecha_hora().toLocalDate(), k -> new int[3]);
            if ("Atendida".equals(c.getEstado())) a[0]++;
            else if ("No asistió".equals(c.getEstado())) a[1]++;
            else if ("Cancelada".equals(c.getEstado())) a[2]++;
        }
        int at = 0, na = 0, ca = 0;
        for (var e : porDia.entrySet()) {
            int[] a = e.getValue();
            t.fila(e.getKey().format(DIA), String.valueOf(a[0]), String.valueOf(a[1]), String.valueOf(a[2]), porcentaje(a[0], a[0] + a[1]));
            at += a[0]; na += a[1]; ca += a[2];
        }
        t.dato("Citas atendidas", String.valueOf(at));
        t.dato("Citas con inasistencia", String.valueOf(na));
        t.dato("Citas canceladas", String.valueOf(ca));
        t.dato("% de asistencia (atendidas / atendidas + no asistió)", porcentaje(at, at + na));
        return t;
    }

    private String porcentaje(int parte, int total) {
        return total == 0 ? "—" : new BigDecimal(parte * 100.0 / total).setScale(1, RoundingMode.HALF_UP) + " %";
    }

    private Tabla inventario() {
        Tabla t = new Tabla();
        t.titulo = "Inventario con semáforo";
        t.subtitulo = "Situación al " + LocalDate.now().format(DIA);
        t.columnas = List.of("Producto", "Tipo", "Stock", "Mínimo", "Próx. vencimiento", "Uds. vencidas / por vencer (30 d)", "Semáforo");

        List<Producto> activos = productoRepositorio.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo())).toList();
        int rojos = 0, amarillos = 0, verdes = 0;
        Map<Integer, Integer> enRiesgo = new HashMap<>();
        for (LoteProducto l : loteRepositorio.findAll()) {
            if (l.getStock_actual() == null || l.getStock_actual() <= 0 || l.getFecha_vencimiento() == null) continue;
            if (!l.getFecha_vencimiento().isAfter(LocalDate.now().plusDays(30))) enRiesgo.merge(l.getProducto_id(), l.getStock_actual(), Integer::sum);
        }
        List<Object[]> filas = new ArrayList<>();
        for (Producto p : activos) {
            int stock = p.getStock() == null ? 0 : p.getStock();
            int min = p.getStock_minimo() == null ? 5 : p.getStock_minimo();
            LocalDate v = p.getFecha_vencimiento();
            String color, texto;
            if (stock <= 0 || (v != null && v.isBefore(LocalDate.now()))) {
                color = "rojo"; texto = stock <= 0 ? "Crítico: sin stock" : "Crítico: vencido";
            } else if (stock <= min || (v != null && !v.isAfter(LocalDate.now().plusDays(30)))) {
                color = "amarillo"; texto = stock <= min ? "Alerta: stock bajo" : "Alerta: por vencer";
            } else {
                color = "verde"; texto = "Normal";
            }
            switch (color) { case "rojo" -> rojos++; case "amarillo" -> amarillos++; default -> verdes++; }
            filas.add(new Object[]{color, p.getNombre(), p.getTipo() == null ? "—" : p.getTipo(), String.valueOf(stock), String.valueOf(min),
                    v == null ? "—" : v.format(DIA), String.valueOf(enRiesgo.getOrDefault(p.getId(), 0)), texto});
        }
        List<String> orden = List.of("rojo", "amarillo", "verde");
        filas.sort(Comparator.comparingInt((Object[] f) -> orden.indexOf((String) f[0])).thenComparing(f -> String.valueOf(f[1])));
        for (Object[] f : filas) {
            t.filaColor((String) f[0], (String) f[1], (String) f[2], (String) f[3], (String) f[4], (String) f[5], (String) f[6], (String) f[7]);
        }
        t.dato("Críticos (rojo)", String.valueOf(rojos));
        t.dato("En alerta (amarillo)", String.valueOf(amarillos));
        t.dato("Normales (verde)", String.valueOf(verdes));
        return t;
    }

    private Tabla comprobantes(LocalDate d, LocalDate h) {
        Tabla t = new Tabla();
        t.titulo = "Comprobantes emitidos";
        t.subtitulo = rango(d, h);
        t.columnas = List.of("N° comprobante", "Tipo", "Cliente", "Concepto", "Método", "Monto", "Fecha");

        Map<Integer, Pago> pagos = new HashMap<>();
        for (Pago p : pagoRepositorio.findAll()) pagos.put(p.getId(), p);
        Map<Integer, String> nombres = new HashMap<>();
        for (Usuario u : usuarioRepositorio.findAll()) nombres.put(u.getId(), u.getNombre());

        List<Comprobante> lista = new ArrayList<>(comprobanteRepositorio.findAll());
        lista.sort(Comparator.comparing(Comprobante::getId));
        BigDecimal total = BigDecimal.ZERO;
        int n = 0;
        for (Comprobante c : lista) {
            Pago p = c.getPago_id() == null ? null : pagos.get(c.getPago_id());
            if (p == null || p.getFecha() == null || !enRango(p.getFecha().toLocalDate(), d, h)) continue;
            String concepto = p.getCita_id() != null ? "Cita #" + p.getCita_id() : (p.getPedido_id() != null ? "Pedido #" + p.getPedido_id() : "—");
            t.fila(c.getNumero() == null ? "#" + c.getId() : c.getNumero(), c.getTipo() == null ? "Boleta" : c.getTipo(),
                    nombres.getOrDefault(p.getUsuario_id(), "N/D"), concepto, p.getMetodo() == null ? "—" : p.getMetodo(),
                    soles(p.getMonto()), p.getFecha().format(DIA_HORA));
            if (p.getMonto() != null) total = total.add(p.getMonto());
            n++;
        }
        t.dato("Comprobantes emitidos", String.valueOf(n));
        t.dato("Monto total", soles(total));
        return t;
    }
}

package com.vamospet.backend.servicio;
import com.vamospet.backend.modelo.LoteProducto;
import com.vamospet.backend.modelo.MovimientoStock;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.LoteProductoRepository;
import com.vamospet.backend.repositorio.MovimientoStockRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventarioService {

    private static final ImmutableSet<String> TIPOS_MOVIMIENTO = ImmutableSet.of("Entrada", "Salida", "Ajuste");
    public static final String LOTE_INICIAL = "LOTE-INICIAL";
    public static final String LOTE_SIN_DATOS = "SIN-LOTE";

    @Autowired
    private ProductoRepository productoRepositorio;

    @Autowired
    private MovimientoStockRepository movimientoRepositorio;

    @Autowired
    private LoteProductoRepository loteRepositorio;

    @Autowired
    private UsuarioRepository usuarioRepositorio;

    @Autowired
    private NotificacionService notificacionService;

    @Transactional
    public MovimientoStock registrarMovimiento(Integer productoId, String tipo, Integer cantidad, String motivo, Integer usuarioId) {
        return registrarMovimiento(productoId, tipo, cantidad, motivo, usuarioId, null, null);
    }

    @Transactional
    public MovimientoStock registrarMovimiento(Integer productoId, String tipo, Integer cantidad, String motivo,
                                               Integer usuarioId, String numeroLote, LocalDate fechaVencimiento) {
        Preconditions.checkArgument(productoId != null, "El producto es obligatorio.");
        Preconditions.checkArgument(TIPOS_MOVIMIENTO.contains(tipo),
                "Tipo de movimiento inválido: %s (use Entrada, Salida o Ajuste).", tipo);
        Preconditions.checkArgument(cantidad != null && cantidad != 0, "La cantidad debe ser distinta de cero.");
        String lote = numeroLote == null || numeroLote.isBlank() ? null : numeroLote.trim();
        Preconditions.checkArgument(lote == null || lote.length() <= 60, "El número de lote admite hasta 60 caracteres.");

        Producto producto = productoRepositorio.findById(productoId).orElse(null);
        if (producto == null) return null;

        int stockAntes = producto.getStock() != null ? producto.getStock() : 0;
        int delta;

        switch (tipo) {
            case "Entrada" -> delta = Math.abs(cantidad);
            case "Salida" -> delta = -Math.abs(cantidad);
            default -> delta = cantidad;
        }

        int stockNuevo = stockAntes + delta;
        Preconditions.checkState(delta >= 0 || stockNuevo >= 0,
                "Stock insuficiente de \"%s\": hay %s y se intentó descontar %s.",
                producto.getNombre(), stockAntes, Math.abs(delta));

        List<LoteProducto> lotes = new ArrayList<>(loteRepositorio.delProducto(productoId));
        Set<LoteProducto> tocados = new LinkedHashSet<>();
        cuadrarLotesConStock(producto, Math.max(stockAntes, 0), lotes, tocados);

        LoteProducto principal = null;
        StringBuilder detalle = new StringBuilder();
        if (delta > 0) {
            Preconditions.checkArgument(fechaVencimiento == null || !fechaVencimiento.isBefore(LocalDate.now()),
                    "No se puede ingresar un lote ya vencido (%s).", fechaVencimiento);
            String nombreLote = lote != null ? lote : LOTE_SIN_DATOS;
            LoteProducto l = lotes.stream().filter(x -> nombreLote.equalsIgnoreCase(x.getNumero_lote())).findFirst().orElse(null);
            if (l == null) {
                l = nuevoLote(productoId, nombreLote, fechaVencimiento);
                lotes.add(l);
            } else if (fechaVencimiento != null) {
                Preconditions.checkArgument(l.getFecha_vencimiento() == null || l.getFecha_vencimiento().equals(fechaVencimiento),
                        "El lote %s ya existe con otra fecha de vencimiento (%s).", nombreLote, l.getFecha_vencimiento());
                l.setFecha_vencimiento(fechaVencimiento);
            }
            l.setStock_actual(l.getStock_actual() + delta);
            tocados.add(l);
            principal = l;
            detalle.append(nombreLote).append(" x").append(delta);
        } else {
            int aDescontar = -delta;
            boolean soloVigentes = "Salida".equals(tipo);
            if (soloVigentes) {
                int vendible = lotes.stream().filter(InventarioService::vigente).mapToInt(LoteProducto::getStock_actual).sum();
                int vencidas = Math.max(0, Math.max(stockAntes, 0) - vendible);
                Preconditions.checkState(vendible >= aDescontar,
                        "Stock insuficiente de \"%s\": hay %s disponibles para la venta (%s unidades vencidas no se pueden vender) y se intentó descontar %s.",
                        producto.getNombre(), vendible, vencidas, aDescontar);
            }
            List<LoteProducto> ordenados = lotes.stream()
                    .filter(l -> l.getStock_actual() > 0)
                    .filter(l -> !soloVigentes || vigente(l))
                    .sorted(ORDEN_FEFO)
                    .toList();
            int pendiente = aDescontar;
            for (LoteProducto l : ordenados) {
                if (pendiente == 0) break;
                int toma = Math.min(l.getStock_actual(), pendiente);
                l.setStock_actual(l.getStock_actual() - toma);
                pendiente -= toma;
                tocados.add(l);
                if (principal == null) principal = l;
                if (detalle.length() > 0) detalle.append(", ");
                detalle.append(l.getNumero_lote()).append(" x").append(toma);
            }
        }

        for (LoteProducto l : tocados) loteRepositorio.save(l);

        producto.setStock(stockNuevo);
        if (stockNuevo > 0) {
            producto.setFecha_vencimiento(lotes.stream()
                    .filter(l -> l.getStock_actual() > 0 && l.getFecha_vencimiento() != null)
                    .map(LoteProducto::getFecha_vencimiento).min(Comparator.naturalOrder()).orElse(null));
        }

        if (stockNuevo == 0 && !"Descontinuado".equals(producto.getEstado())) {
            producto.setEstado("Agotado");
        } else if (stockNuevo > 0 && "Agotado".equals(producto.getEstado())) {
            producto.setEstado("Activo");
        }

        productoRepositorio.save(producto);

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProducto_id(productoId);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(Math.abs(cantidad));
        String texto = (motivo == null ? "" : motivo) + (detalle.length() > 0 ? " [lote " + detalle + "]" : "");
        movimiento.setMotivo(texto.length() > 255 ? texto.substring(0, 255) : texto);
        movimiento.setUsuario_id(usuarioId);
        movimiento.setLote_id(principal != null ? principal.getId() : null);
        MovimientoStock guardado = movimientoRepositorio.save(movimiento);

        Integer minimo = producto.getStock_minimo() != null ? producto.getStock_minimo() : 5;
        boolean cruzoElUmbral = stockAntes > minimo && stockNuevo <= minimo;
        if (cruzoElUmbral) {
            notificarStockBajoAPersonalInterno(producto);
        }

        return guardado;
    }

    public int stockVendible(Producto producto) {
        int stock = producto.getStock() != null ? Math.max(producto.getStock(), 0) : 0;
        List<LoteProducto> lotes = loteRepositorio.delProducto(producto.getId());
        int enLotes = lotes.stream().mapToInt(LoteProducto::getStock_actual).sum();
        int vigentes = lotes.stream().filter(InventarioService::vigente).mapToInt(LoteProducto::getStock_actual).sum();
        int sinLote = Math.max(0, stock - enLotes);
        boolean sinLoteVigente = producto.getFecha_vencimiento() == null || !producto.getFecha_vencimiento().isBefore(LocalDate.now());
        return Math.min(stock, vigentes + (sinLoteVigente ? sinLote : 0));
    }

    public List<LoteProducto> lotesPorVencer(int dias) {
        LocalDate limite = LocalDate.now().plusDays(dias);
        return loteRepositorio.findAll().stream()
                .filter(l -> l.getStock_actual() != null && l.getStock_actual() > 0)
                .filter(l -> l.getFecha_vencimiento() != null && !l.getFecha_vencimiento().isAfter(limite))
                .sorted(Comparator.comparing(LoteProducto::getFecha_vencimiento))
                .toList();
    }

    static boolean vigente(LoteProducto l) {
        return l.getFecha_vencimiento() == null || !l.getFecha_vencimiento().isBefore(LocalDate.now());
    }

    private static final Comparator<LoteProducto> ORDEN_FEFO = Comparator
            .comparing(LoteProducto::getFecha_vencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(l -> l.getId() == null ? Integer.MAX_VALUE : l.getId());

    private static LoteProducto nuevoLote(Integer productoId, String numero, LocalDate vencimiento) {
        LoteProducto l = new LoteProducto();
        l.setProducto_id(productoId);
        l.setNumero_lote(numero);
        l.setFecha_vencimiento(vencimiento);
        l.setStock_actual(0);
        return l;
    }

    private void cuadrarLotesConStock(Producto producto, int stock, List<LoteProducto> lotes, Set<LoteProducto> tocados) {
        int suma = lotes.stream().mapToInt(LoteProducto::getStock_actual).sum();
        if (stock > suma) {
            LoteProducto inicial = lotes.stream().filter(l -> LOTE_INICIAL.equals(l.getNumero_lote())).findFirst().orElse(null);
            if (inicial == null) {
                inicial = nuevoLote(producto.getId(), LOTE_INICIAL, producto.getFecha_vencimiento());
                lotes.add(inicial);
            }
            inicial.setStock_actual(inicial.getStock_actual() + (stock - suma));
            tocados.add(inicial);
        } else if (stock < suma) {
            int sobrante = suma - stock;
            for (LoteProducto l : lotes.stream().filter(x -> x.getStock_actual() > 0).sorted(ORDEN_FEFO).toList()) {
                if (sobrante == 0) break;
                int toma = Math.min(l.getStock_actual(), sobrante);
                l.setStock_actual(l.getStock_actual() - toma);
                sobrante -= toma;
                tocados.add(l);
            }
        }
    }

    private void notificarStockBajoAPersonalInterno(Producto producto) {
        java.util.List<Usuario> personal = usuarioRepositorio.findAll().stream()
                .filter(u -> u.getRol_id() != null && u.getRol_id() == 1)
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .toList();

        String mensaje = "El producto \"" + producto.getNombre() + "\" quedó con stock bajo ("
                + producto.getStock() + " unidades, mínimo configurado: " + producto.getStock_minimo() + ").";

        for (Usuario u : personal) {
            notificacionService.notificar(u.getId(), "Stock bajo: " + producto.getNombre(), mensaje, "Inventario");
        }
    }
}

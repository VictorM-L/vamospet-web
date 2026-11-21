package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Pedido;
import com.vamospet.backend.modelo.PedidoDetalle;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.modelo.Receta;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.modelo.Devolucion;
import com.vamospet.backend.modelo.DevolucionDetalle;
import com.vamospet.backend.repositorio.DevolucionDetalleRepository;
import com.vamospet.backend.repositorio.DevolucionRepository;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.PagoRepository;
import com.vamospet.backend.repositorio.PedidoDetalleRepository;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VentaService {

    public static final int DIAS_VIGENCIA_RECETA = 30;
    public static final int MAXIMO_POR_PRODUCTO = 99;
    public static final String WEB = "Web";
    public static final String MOSTRADOR = "Mostrador";
    public static final String CANCELADO = "Cancelado";
    public static final String ENTREGADO = "Entregado";
    public static final String USAR_DEVOLUCION = "Para este pedido usa una devolución";

    public record Item(Integer producto_id, Integer cantidad) {}

    public record Pedir(Integer cliente_id, List<Item> items, Integer receta_id, String metodo_entrega) {}

    public record Resultado(Pedido pedido, List<PedidoDetalle> detalle) {}

    @Autowired private PedidoRepository pedidoRepositorio;
    @Autowired private PedidoDetalleRepository detalleRepositorio;
    @Autowired private ProductoRepository productoRepositorio;
    @Autowired private RecetaRepository recetaRepositorio;
    @Autowired private MascotaRepository mascotaRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private InventarioService inventarioService;
    @Autowired private PagoRepository pagoRepositorio;
    @Autowired private DevolucionRepository devolucionRepositorio;
    @Autowired private DevolucionDetalleRepository devolucionDetalleRepositorio;

    public static LocalDateTime inicioVigencia(Receta r) {
        return r.getAprobada_en() != null ? r.getAprobada_en() : r.getFecha();
    }

    public static boolean vigente(Receta r, LocalDateTime ahora) {
        LocalDateTime inicio = inicioVigencia(r);
        return inicio != null && !ahora.isAfter(inicio.plusDays(DIAS_VIGENCIA_RECETA));
    }

    public static String motivoNoUsable(Receta r, Integer clienteId, Mascota mascota, LocalDateTime ahora) {
        if (r == null) return "La receta elegida no existe.";
        if (mascota == null || clienteId == null || !clienteId.equals(mascota.getUsuario_id())) return "La receta elegida no es de una mascota de este cliente.";
        if (!"Aprobada".equals(r.getEstado())) return "La receta todavía no está aprobada por un veterinario.";
        if (Boolean.TRUE.equals(r.getUsada())) return "Esa receta ya se usó en otra compra.";
        if (!vigente(r, ahora)) return "La receta venció: tiene " + DIAS_VIGENCIA_RECETA + " días de vigencia desde su aprobación.";
        return null;
    }

    public List<Map<String, Object>> recetasUsables(Integer clienteId) {
        LocalDateTime ahora = LocalDateTime.now();
        Map<Integer, Mascota> mascotas = mascotaRepositorio.buscarPorDueno(clienteId).stream()
                .collect(Collectors.toMap(Mascota::getId, m -> m, (a, b) -> a, LinkedHashMap::new));
        List<Map<String, Object>> lista = new ArrayList<>();
        recetaRepositorio.findAll().stream()
                .filter(r -> r.getMascota_id() != null && mascotas.containsKey(r.getMascota_id()))
                .filter(r -> motivoNoUsable(r, clienteId, mascotas.get(r.getMascota_id()), ahora) == null)
                .sorted(Comparator.comparing(VentaService::inicioVigencia).reversed())
                .forEach(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("mascota_id", r.getMascota_id());
                    m.put("mascota", mascotas.get(r.getMascota_id()).getNombre());
                    m.put("contenido", r.getContenido());
                    m.put("aprobada_en", inicioVigencia(r));
                    m.put("vence", inicioVigencia(r).plusDays(DIAS_VIGENCIA_RECETA).toLocalDate());
                    lista.add(m);
                });
        return lista;
    }

    @Transactional
    public synchronized Resultado vender(Integer actorId, Pedir p) {
        Usuario actor = autorizacion.conRol(actorId, AutorizacionService.CLIENTE, AutorizacionService.AUXILIAR);
        if (actor == null) throw new NegocioException(403, "Solo el cliente (en la web) o el Auxiliar (en mostrador) registran una venta.");
        boolean mostrador = Integer.valueOf(AutorizacionService.AUXILIAR).equals(actor.getRol_id());
        Integer clienteId = mostrador ? (p == null ? null : p.cliente_id()) : actor.getId();
        if (mostrador) {
            Usuario cliente = clienteId == null ? null : usuarioRepositorio.findById(clienteId).orElse(null);
            if (cliente == null || !Integer.valueOf(AutorizacionService.CLIENTE).equals(cliente.getRol_id())) {
                throw new NegocioException(400, "Elige al cliente de la venta.");
            }
        }
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        if (p != null && p.items() != null) {
            for (Item i : p.items()) {
                if (i == null || i.producto_id() == null || i.cantidad() == null || i.cantidad() <= 0) {
                    throw new NegocioException(400, "Cada producto necesita una cantidad mayor a cero.");
                }
                cantidades.merge(i.producto_id(), i.cantidad(), Integer::sum);
            }
        }
        if (cantidades.isEmpty()) throw new NegocioException(400, "El carrito está vacío.");

        List<String> faltantes = new ArrayList<>();
        List<String> conReceta = new ArrayList<>();
        Map<Integer, Producto> productos = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            Producto prod = productoRepositorio.findById(e.getKey()).orElse(null);
            if (prod == null || Boolean.FALSE.equals(prod.getActivo()) || "Descontinuado".equals(prod.getEstado())) {
                throw new NegocioException(409, "Uno de los productos ya no está a la venta. Actualiza tu carrito.");
            }
            if (e.getValue() > MAXIMO_POR_PRODUCTO) throw new NegocioException(400, "Máximo " + MAXIMO_POR_PRODUCTO + " unidades de " + prod.getNombre() + ".");
            int disponible = inventarioService.stockVendible(prod);
            if (disponible < e.getValue()) faltantes.add(InventarioService.describirFaltante(prod.getNombre(), e.getValue(), disponible, prod.getStock()));
            if (Boolean.TRUE.equals(prod.getRequiere_receta())) conReceta.add(prod.getNombre());
            productos.put(prod.getId(), prod);
        }
        if (!faltantes.isEmpty()) throw new NegocioException(409, InventarioService.mensajeSinStock(faltantes));

        Receta receta = null;
        if (!conReceta.isEmpty()) {
            if (p.receta_id() == null) {
                throw new NegocioException(409, "Necesitas una receta aprobada y vigente para: " + String.join(", ", conReceta) + ".");
            }
            receta = recetaRepositorio.findById(p.receta_id()).orElse(null);
            Mascota mascota = receta == null || receta.getMascota_id() == null ? null : mascotaRepositorio.findById(receta.getMascota_id()).orElse(null);
            String motivo = motivoNoUsable(receta, clienteId, mascota, LocalDateTime.now());
            if (motivo != null) throw new NegocioException(409, motivo);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            total = total.add(productos.get(e.getKey()).getPrecio().multiply(BigDecimal.valueOf(e.getValue())));
        }
        Pedido pedido = new Pedido();
        pedido.setUsuario_id(clienteId);
        pedido.setCanal(mostrador ? MOSTRADOR : WEB);
        pedido.setEstado("Pendiente");
        String entrega = p.metodo_entrega() == null || p.metodo_entrega().isBlank() ? "Recojo en tienda" : p.metodo_entrega().trim();
        pedido.setMetodo_entrega(entrega.length() > 50 ? entrega.substring(0, 50) : entrega);
        pedido.setTotal(total);
        pedido.setFecha(LocalDateTime.now());
        pedido = pedidoRepositorio.save(pedido);

        if (receta != null && recetaRepositorio.marcarUsada(receta.getId(), pedido.getId(), LocalDateTime.now()) != 1) {
            throw new NegocioException(409, "Esa receta ya se usó en otra compra.");
        }

        List<PedidoDetalle> detalle = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            Producto prod = productos.get(e.getKey());
            PedidoDetalle d = new PedidoDetalle();
            d.setPedido_id(pedido.getId());
            d.setProducto_id(prod.getId());
            d.setCantidad(e.getValue());
            d.setPrecio_unitario(prod.getPrecio());
            d.setReceta_id(Boolean.TRUE.equals(prod.getRequiere_receta()) && receta != null ? receta.getId() : null);
            detalle.add(detalleRepositorio.save(d));
            inventarioService.registrarMovimiento(prod.getId(), "Salida", e.getValue(),
                    (mostrador ? "Venta en mostrador" : "Venta web") + " - Pedido #" + pedido.getId(), actor.getId());
        }
        return new Resultado(pedido, detalle);
    }

    @Transactional
    public synchronized Pedido anularVentaMostrador(Integer actorId, Integer pedidoId) {
        if (autorizacion.conRol(actorId, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "Solo el Auxiliar anula una venta de mostrador sin cobrar.");
        }
        Pedido pedido = pedidoId == null ? null : pedidoRepositorio.findById(pedidoId).orElse(null);
        if (pedido == null) throw new NegocioException(404, "El pedido no existe.");
        if (!MOSTRADOR.equals(pedido.getCanal())) throw new NegocioException(409, "Solo se anulan aquí las ventas de mostrador.");
        if (!"Pendiente".equals(pedido.getEstado())) {
            throw new NegocioException(409, "Esta venta ya no se puede anular (estado: " + pedido.getEstado() + ").");
        }
        if (tienePago(pedidoId)) throw new NegocioException(409, "Esta venta ya tiene un pago registrado; no se puede anular.");
        return cancelarYReponer(pedido, actorId, "Venta en mostrador anulada sin cobrar");
    }

    @Transactional
    public synchronized Pedido cancelarPedido(Integer actorId, Integer pedidoId) {
        Pedido pedido = pedidoId == null ? null : pedidoRepositorio.findById(pedidoId).orElse(null);
        if (pedido == null) throw new NegocioException(404, "El pedido no existe.");
        if (CANCELADO.equals(pedido.getEstado())) return pedido;
        if (ENTREGADO.equals(pedido.getEstado()) || tienePago(pedidoId)) throw new NegocioException(409, USAR_DEVOLUCION);
        return cancelarYReponer(pedido, actorId, "Pedido cancelado");
    }

    private boolean tienePago(Integer pedidoId) {
        return pagoRepositorio.findAll().stream()
                .anyMatch(pg -> pedidoId.equals(pg.getPedido_id()) && !PagoService.RECHAZADO.equals(pg.getEstado()));
    }

    public Map<Integer, Integer> pendientesDeReponer(Integer pedidoId) {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (PedidoDetalle d : detalleRepositorio.findAll()) {
            if (!pedidoId.equals(d.getPedido_id()) || d.getProducto_id() == null || d.getCantidad() == null) continue;
            cantidades.merge(d.getProducto_id(), d.getCantidad(), Integer::sum);
        }
        Set<Integer> devoluciones = devolucionRepositorio.findAll().stream()
                .filter(dv -> pedidoId.equals(dv.getPedido_id())).map(Devolucion::getId).collect(Collectors.toSet());
        if (!devoluciones.isEmpty()) {
            for (DevolucionDetalle dd : devolucionDetalleRepositorio.findAll()) {
                if (dd.getDevolucion_id() != null && devoluciones.contains(dd.getDevolucion_id()) && dd.getProducto_id() != null && dd.getCantidad() != null) {
                    cantidades.computeIfPresent(dd.getProducto_id(), (k, v) -> v - dd.getCantidad());
                }
            }
        }
        cantidades.values().removeIf(v -> v <= 0);
        return cantidades;
    }

    private Pedido cancelarYReponer(Pedido pedido, Integer actorId, String motivo) {
        for (Map.Entry<Integer, Integer> e : pendientesDeReponer(pedido.getId()).entrySet()) {
            inventarioService.registrarMovimiento(e.getKey(), "Entrada", e.getValue(), motivo + " - Pedido #" + pedido.getId(), actorId);
        }
        pedido.setEstado(CANCELADO);
        Pedido guardado = pedidoRepositorio.save(pedido);
        recetaRepositorio.liberarDePedido(pedido.getId());
        return guardado;
    }
}

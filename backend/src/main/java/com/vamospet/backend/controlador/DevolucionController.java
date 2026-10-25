package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.*;
import com.vamospet.backend.repositorio.*;
import com.vamospet.backend.servicio.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = "*")
public class DevolucionController {

    @Autowired private DevolucionRepository devolucionRepositorio;
    @Autowired private DevolucionDetalleRepository detalleRepositorio;
    @Autowired private PedidoRepository pedidoRepositorio;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private InventarioService inventarioService;

    @GetMapping
    public List<Devolucion> listar() {
        return devolucionRepositorio.findAll().stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha())).toList();
    }

    @GetMapping("/{id}/detalle")
    public List<DevolucionDetalle> detalle(@PathVariable Integer id) {
        return detalleRepositorio.findAll().stream().filter(d -> id.equals(d.getDevolucion_id())).toList();
    }

    @GetMapping("/pedido/{pedidoId}/disponible")
    public ResponseEntity<?> disponible(@PathVariable Integer pedidoId) {
        Pedido pedido = pedidoRepositorio.findById(pedidoId).orElse(null);
        if (pedido == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No existe el pedido #" + pedidoId + "."));
        if (!"Entregado".equals(pedido.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se pueden devolver productos de pedidos Entregados."));
        }
        return ResponseEntity.ok(Map.of("pedido", pedido, "lineas", lineasDisponibles(pedidoId)));
    }

    private List<Map<String, Object>> lineasDisponibles(Integer pedidoId) {
        Map<Integer, Integer> devueltas = new HashMap<>();
        Set<Integer> idsDev = new HashSet<>();
        for (Devolucion d : devolucionRepositorio.findAll()) if (pedidoId.equals(d.getPedido_id())) idsDev.add(d.getId());
        for (DevolucionDetalle dd : detalleRepositorio.findAll()) {
            if (idsDev.contains(dd.getDevolucion_id())) devueltas.merge(dd.getProducto_id(), dd.getCantidad(), Integer::sum);
        }
        Map<Integer, Integer> vendidas = new LinkedHashMap<>();
        Map<Integer, BigDecimal> precios = new HashMap<>();
        for (PedidoDetalle pd : pedidoDetalleRepositorio.findAll()) {
            if (!pedidoId.equals(pd.getPedido_id())) continue;
            vendidas.merge(pd.getProducto_id(), pd.getCantidad(), Integer::sum);
            precios.put(pd.getProducto_id(), pd.getPrecio_unitario());
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (var e : vendidas.entrySet()) {
            int dev = devueltas.getOrDefault(e.getKey(), 0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("producto_id", e.getKey());
            m.put("vendida", e.getValue());
            m.put("devuelta", dev);
            m.put("disponible", e.getValue() - dev);
            m.put("precio_unitario", precios.get(e.getKey()));
            res.add(m);
        }
        return res;
    }

    @PostMapping
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        try {
            Integer pedidoId = body.get("pedido_id") == null ? null : Integer.valueOf(body.get("pedido_id").toString());
            Integer usuarioId = body.get("usuario_id") == null ? null : Integer.valueOf(body.get("usuario_id").toString());
            String motivo = body.get("motivo") == null ? "" : body.get("motivo").toString().trim();
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            Usuario admin = usuarioId == null ? null : usuarioRepositorio.findById(usuarioId).orElse(null);
            if (admin == null || !Integer.valueOf(1).equals(admin.getRol_id())) {
                return error(HttpStatus.FORBIDDEN, "Solo el Administrador puede registrar devoluciones.");
            }
            Pedido pedido = pedidoId == null ? null : pedidoRepositorio.findById(pedidoId).orElse(null);
            if (pedido == null) return error(HttpStatus.BAD_REQUEST, "Pedido no encontrado.");
            if (!"Entregado".equals(pedido.getEstado())) return error(HttpStatus.BAD_REQUEST, "Solo se pueden devolver productos de pedidos Entregados.");
            if (motivo.isBlank()) return error(HttpStatus.BAD_REQUEST, "Indica el motivo de la devolución.");
            if (items == null || items.isEmpty()) return error(HttpStatus.BAD_REQUEST, "Selecciona al menos un producto a devolver.");

            Map<Integer, Map<String, Object>> disp = new HashMap<>();
            for (Map<String, Object> l : lineasDisponibles(pedidoId)) disp.put((Integer) l.get("producto_id"), l);

            List<DevolucionDetalle> lineas = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            Set<Integer> repetidos = new HashSet<>();
            for (Map<String, Object> it : items) {
                Integer productoId = Integer.valueOf(it.get("producto_id").toString());
                Integer cantidad = Integer.valueOf(it.get("cantidad").toString());
                Map<String, Object> l = disp.get(productoId);
                if (l == null) return error(HttpStatus.BAD_REQUEST, "El producto " + productoId + " no se vendió en ese pedido.");
                if (!repetidos.add(productoId)) return error(HttpStatus.BAD_REQUEST, "Producto repetido en la devolución.");
                if (cantidad <= 0 || cantidad > (Integer) l.get("disponible")) {
                    return error(HttpStatus.BAD_REQUEST, "Cantidad inválida para el producto " + productoId + " (máximo " + l.get("disponible") + ").");
                }
                DevolucionDetalle d = new DevolucionDetalle();
                d.setProducto_id(productoId);
                d.setCantidad(cantidad);
                d.setPrecio_unitario((BigDecimal) l.get("precio_unitario"));
                lineas.add(d);
                total = total.add(d.getPrecio_unitario().multiply(BigDecimal.valueOf(cantidad)));
            }

            Devolucion dev = new Devolucion();
            dev.setPedido_id(pedidoId);
            dev.setUsuario_id(usuarioId);
            dev.setMotivo(motivo);
            dev.setTotal_reembolso(total);
            dev = devolucionRepositorio.save(dev);

            for (DevolucionDetalle d : lineas) {
                d.setDevolucion_id(dev.getId());
                detalleRepositorio.save(d);
                inventarioService.registrarMovimiento(d.getProducto_id(), "Entrada", d.getCantidad(),
                        "Devolución #" + dev.getId() + " (pedido #" + pedidoId + ")", usuarioId);
            }
            return ResponseEntity.ok(dev);
        } catch (ClassCastException | NumberFormatException | NullPointerException e) {
            return error(HttpStatus.BAD_REQUEST, "Datos de devolución inválidos.");
        }
    }

    private ResponseEntity<?> error(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg));
    }
}

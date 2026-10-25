package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.PedidoDetalle;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.repositorio.PedidoDetalleRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.servicio.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedido-detalle")
@CrossOrigin(origins = "*")
public class PedidoDetalleController {

    @Autowired
    private PedidoDetalleRepository repositorio;

    @Autowired
    private ProductoRepository productoRepositorio;

    @Autowired
    private InventarioService inventarioService;

    @GetMapping("/pedido/{pedidoId}")
    public List<PedidoDetalle> listarPorPedido(@PathVariable Integer pedidoId) {
        return repositorio.findAll().stream()
                .filter(d -> d.getPedido_id() != null && d.getPedido_id().equals(pedidoId))
                .toList();
    }

    public record ItemStock(Integer producto_id, Integer cantidad) {}

    @PostMapping("/validar-stock")
    public ResponseEntity<?> validarStock(@RequestBody List<ItemStock> items) {
        Map<Integer, Integer> pedido = new LinkedHashMap<>();
        for (ItemStock i : items) {
            if (i.producto_id() == null || i.cantidad() == null || i.cantidad() <= 0) continue;
            pedido.merge(i.producto_id(), i.cantidad(), Integer::sum);
        }
        List<String> faltantes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : pedido.entrySet()) {
            Producto p = productoRepositorio.findById(e.getKey()).orElse(null);
            int disponible = p == null ? 0 : inventarioService.stockVendible(p);
            if (p == null || disponible < e.getValue()) {
                faltantes.add((p != null ? p.getNombre() : "Producto #" + e.getKey())
                        + " (quedan " + disponible + ")");
            }
        }
        if (!faltantes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "No hay stock suficiente de: " + String.join(", ", faltantes)));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping
    @Transactional
    public PedidoDetalle guardar(@RequestBody PedidoDetalle detalle) {
        PedidoDetalle guardado = repositorio.save(detalle);

        if (guardado.getProducto_id() != null && guardado.getCantidad() != null) {
            inventarioService.registrarMovimiento(
                    guardado.getProducto_id(),
                    "Salida",
                    guardado.getCantidad(),
                    "Venta - Pedido #" + guardado.getPedido_id(),
                    null
            );
        }

        return guardado;
    }
}

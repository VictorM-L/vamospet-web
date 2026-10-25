package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.PedidoDetalle;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.repositorio.PedidoDetalleRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {

    @Autowired
    private ProductoRepository productoRepositorio;

    @Autowired
    private PedidoDetalleRepository pedidoDetalleRepositorio;

    @GetMapping("/mas-vendidos")
    public List<Map<String, Object>> masVendidos(@RequestParam(required = false) String tipo) {
        List<Producto> productos = productoRepositorio.findAll();
        Map<Integer, Producto> productosPorId = productos.stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        Map<Integer, Integer> vendidosPorProducto = new HashMap<>();
        for (PedidoDetalle d : pedidoDetalleRepositorio.findAll()) {
            if (d.getProducto_id() == null || d.getCantidad() == null) continue;
            Producto p = productosPorId.get(d.getProducto_id());
            if (p == null) continue;
            if (tipo != null && !tipo.isBlank() && !tipo.equals(p.getTipo())) continue;
            vendidosPorProducto.merge(d.getProducto_id(), d.getCantidad(), Integer::sum);
        }

        return vendidosPorProducto.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(15)
                .map(e -> {
                    Producto p = productosPorId.get(e.getKey());
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("producto_id", e.getKey());
                    fila.put("nombre", p != null ? p.getNombre() : "N/D");
                    fila.put("tipo", p != null ? p.getTipo() : null);
                    fila.put("unidadesVendidas", e.getValue());
                    return fila;
                })
                .toList();
    }

    @GetMapping("/valorizacion")
    public Map<String, Object> valorizacion(@RequestParam(required = false) String tipo) {
        List<Producto> productos = productoRepositorio.findAll().stream()
                .filter(p -> !"Descontinuado".equals(p.getEstado()))
                .filter(p -> tipo == null || tipo.isBlank() || tipo.equals(p.getTipo()))
                .toList();

        BigDecimal totalCosto = BigDecimal.ZERO;
        BigDecimal totalVenta = BigDecimal.ZERO;
        int totalUnidades = 0;

        for (Producto p : productos) {
            int stock = p.getStock() != null ? p.getStock() : 0;
            totalUnidades += stock;
            if (p.getPrecio_costo() != null) totalCosto = totalCosto.add(p.getPrecio_costo().multiply(BigDecimal.valueOf(stock)));
            if (p.getPrecio() != null) totalVenta = totalVenta.add(p.getPrecio().multiply(BigDecimal.valueOf(stock)));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalUnidades", totalUnidades);
        resultado.put("valorCosto", totalCosto);
        resultado.put("valorVenta", totalVenta);
        resultado.put("gananciaPotencial", totalVenta.subtract(totalCosto));
        resultado.put("cantidadProductos", productos.size());
        return resultado;
    }
}

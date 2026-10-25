package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.LoteProducto;
import com.vamospet.backend.modelo.Producto;
import com.vamospet.backend.repositorio.LoteProductoRepository;
import com.vamospet.backend.repositorio.ProductoRepository;
import com.vamospet.backend.servicio.AutorizacionService;
import com.vamospet.backend.servicio.InventarioService;
import com.vamospet.backend.servicio.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/lotes")
@CrossOrigin(origins = "*")
public class LoteController {

    @Autowired private LoteProductoRepository loteRepositorio;
    @Autowired private ProductoRepository productoRepositorio;
    @Autowired private InventarioService inventarioService;
    @Autowired private AutorizacionService autorizacion;

    @GetMapping("/producto/{productoId}")
    public List<Map<String, Object>> delProducto(@PathVariable Integer productoId, @RequestParam Integer usuarioId) {
        exigirAdmin(usuarioId);
        Producto p = productoRepositorio.findById(productoId).orElse(null);
        List<Map<String, Object>> salida = new ArrayList<>();
        loteRepositorio.delProducto(productoId).stream()
                .sorted(Comparator.comparing(LoteProducto::getFecha_vencimiento, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(l -> salida.add(describir(l, p)));
        return salida;
    }

    @GetMapping("/por-vencer")
    public List<Map<String, Object>> porVencer(@RequestParam Integer usuarioId, @RequestParam(defaultValue = "30") int dias) {
        exigirAdmin(usuarioId);
        if (dias < 1 || dias > 365) throw new NegocioException(400, "Los días deben estar entre 1 y 365.");
        Map<Integer, Producto> productos = new HashMap<>();
        productoRepositorio.findAll().forEach(p -> productos.put(p.getId(), p));
        List<Map<String, Object>> salida = new ArrayList<>();
        for (LoteProducto l : inventarioService.lotesPorVencer(dias)) salida.add(describir(l, productos.get(l.getProducto_id())));
        return salida;
    }

    private void exigirAdmin(Integer usuarioId) {
        if (autorizacion.conRol(usuarioId, AutorizacionService.ADMIN) == null) {
            throw new NegocioException(403, "Solo el administrador puede ver los lotes de inventario.");
        }
    }

    private Map<String, Object> describir(LoteProducto l, Producto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("producto_id", l.getProducto_id());
        m.put("producto_nombre", p != null ? p.getNombre() : null);
        m.put("numero_lote", l.getNumero_lote());
        m.put("fecha_vencimiento", l.getFecha_vencimiento());
        m.put("stock_actual", l.getStock_actual());
        m.put("fecha_ingreso", l.getFecha_ingreso());
        Long dias = l.getFecha_vencimiento() == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), l.getFecha_vencimiento());
        m.put("dias_para_vencer", dias);
        m.put("estado", dias == null ? "Sin vencimiento" : dias < 0 ? "Vencido" : dias <= 30 ? "Por vencer" : "Vigente");
        return m;
    }
}

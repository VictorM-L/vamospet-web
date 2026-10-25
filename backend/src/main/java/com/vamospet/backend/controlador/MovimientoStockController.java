package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.MovimientoStock;
import com.vamospet.backend.repositorio.MovimientoStockRepository;
import com.vamospet.backend.servicio.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movimientos-stock")
@CrossOrigin(origins = "*")
public class MovimientoStockController {

    @Autowired
    private MovimientoStockRepository repositorio;

    @Autowired
    private InventarioService inventarioService;

    @GetMapping("/producto/{productoId}")
    public List<MovimientoStock> listarPorProducto(@PathVariable Integer productoId) {
        return repositorio.findAll().stream()
                .filter(m -> productoId.equals(m.getProducto_id()))
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        Integer productoId = (Integer) body.get("producto_id");
        String tipo = (String) body.get("tipo");
        Integer cantidad = (Integer) body.get("cantidad");
        String motivo = (String) body.get("motivo");
        Integer usuarioId = body.get("usuario_id") != null ? (Integer) body.get("usuario_id") : null;

        if (productoId == null || tipo == null || cantidad == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos del movimiento."));
        }

        String numeroLote = body.get("numero_lote") == null ? null : body.get("numero_lote").toString();
        java.time.LocalDate vence = null;
        String venceTexto = body.get("fecha_vencimiento") == null ? "" : body.get("fecha_vencimiento").toString().trim();
        if (!venceTexto.isEmpty()) {
            try { vence = java.time.LocalDate.parse(venceTexto); }
            catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Fecha de vencimiento inválida."));
            }
        }
        MovimientoStock resultado = inventarioService.registrarMovimiento(productoId, tipo, cantidad, motivo, usuarioId, numeroLote, vence);
        if (resultado == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado."));
        }
        return ResponseEntity.ok(resultado);
    }
}

package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Pedido;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.NotificacionService;
import com.vamospet.backend.servicio.NegocioException;
import com.vamospet.backend.servicio.VentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    static final Set<String> ESTADOS = Set.of("Pendiente", "Listo para recoger", "Entregado", VentaService.CANCELADO);

    @Autowired
    private PedidoRepository repositorio;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private VentaService ventaService;

    @GetMapping
    public List<Pedido> listar() { return repositorio.findAll(); }

    @GetMapping("/usuario/{usuarioId}")
    public List<Pedido> listarPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.findAll().stream()
                .filter(p -> p.getUsuario_id() != null && p.getUsuario_id().equals(usuarioId))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> vender(@RequestBody VentaService.Pedir datos) {
        VentaService.Resultado r = ventaService.vender(UsuarioActual.id(), datos);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pedido", r.pedido());
        cuerpo.put("detalle", r.detalle());
        return ResponseEntity.ok(cuerpo);
    }

    @PatchMapping("/{id}/anular")
    public Pedido anular(@PathVariable Integer id) {
        return ventaService.anularVentaMostrador(UsuarioActual.id(), id);
    }

    @PatchMapping("/{id}/estado")
    public Pedido actualizarEstado(@PathVariable Integer id, @RequestBody Pedido datos) {
        Pedido pedidoExistente = repositorio.findById(id).orElse(null);
        if (pedidoExistente == null) throw new NegocioException(404, "El pedido no existe.");
        String nuevo = datos == null ? null : datos.getEstado();
        if (nuevo == null || !ESTADOS.contains(nuevo)) throw new NegocioException(400, "El estado del pedido no es válido.");
        if (VentaService.CANCELADO.equals(pedidoExistente.getEstado())) {
            if (VentaService.CANCELADO.equals(nuevo)) return pedidoExistente;
            throw new NegocioException(409, "Un pedido cancelado no se puede reabrir: registra una venta nueva.");
        }

        Pedido guardado;
        if (VentaService.CANCELADO.equals(nuevo)) {
            guardado = ventaService.cancelarPedido(UsuarioActual.id(), id);
        } else {
            pedidoExistente.setEstado(nuevo);
            guardado = repositorio.save(pedidoExistente);
        }

        if (guardado.getUsuario_id() != null) {
            String mensaje;
            switch (guardado.getEstado()) {
                case "Listo para recoger" -> mensaje = "Tu pedido #" + guardado.getId() + " ya está listo. Puedes pasar a recogerlo por la clínica.";
                case "Entregado" -> mensaje = "Tu pedido #" + guardado.getId() + " fue entregado. ¡Gracias por tu compra!";
                case "Cancelado" -> mensaje = "Tu pedido #" + guardado.getId() + " fue cancelado.";
                default -> mensaje = null;
            }
            if (mensaje != null) {
                notificacionService.notificar(guardado.getUsuario_id(), "Actualización de tu pedido", mensaje, "Pedido");
            }
        }

        return guardado;
    }
}

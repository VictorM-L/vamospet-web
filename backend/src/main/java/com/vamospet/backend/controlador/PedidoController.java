package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Pedido;
import com.vamospet.backend.repositorio.PedidoRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private PedidoRepository repositorio;

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public List<Pedido> listar() { return repositorio.findAll(); }

    @GetMapping("/usuario/{usuarioId}")
    public List<Pedido> listarPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.findAll().stream()
                .filter(p -> p.getUsuario_id() != null && p.getUsuario_id().equals(usuarioId))
                .toList();
    }

    @PostMapping
    public Pedido guardar(@RequestBody Pedido pedido) { return repositorio.save(pedido); }

    @PatchMapping("/{id}/estado")
    public Pedido actualizarEstado(@PathVariable Integer id, @RequestBody Pedido datos) {
        Pedido pedidoExistente = repositorio.findById(id).orElse(null);
        if (pedidoExistente == null) return null;

        pedidoExistente.setEstado(datos.getEstado());
        Pedido guardado = repositorio.save(pedidoExistente);

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

package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Notificacion;
import com.vamospet.backend.repositorio.NotificacionRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {

    @Autowired
    private NotificacionRepository repositorio;

    @Autowired
    private NotificacionService servicio;

    @GetMapping("/usuario/{usuarioId}")
    public List<Notificacion> listarPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.findAll().stream()
                .filter(n -> n.getUsuario_id() != null && n.getUsuario_id().equals(usuarioId))
                .filter(n -> !Boolean.FALSE.equals(n.getEnviado()))
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .toList();
    }

    @PostMapping
    public Notificacion crear(@RequestBody Map<String, String> datos) {
        Integer usuarioId = Integer.parseInt(datos.get("usuario_id"));
        return servicio.notificar(usuarioId, datos.get("titulo"), datos.get("mensaje"), datos.getOrDefault("tipo", "General"));
    }

    @PatchMapping("/{id}/leer")
    public Notificacion marcarLeida(@PathVariable Integer id) {
        Notificacion n = repositorio.findById(id).orElse(null);
        if (n == null) return null;
        n.setLeida(true);
        return repositorio.save(n);
    }

    @PatchMapping("/usuario/{usuarioId}/leer-todas")
    public void marcarTodasLeidas(@PathVariable Integer usuarioId) {
        List<Notificacion> propias = repositorio.findAll().stream()
                .filter(n -> n.getUsuario_id() != null && n.getUsuario_id().equals(usuarioId))
                .toList();
        propias.forEach(n -> n.setLeida(true));
        repositorio.saveAll(propias);
    }
}

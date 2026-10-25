package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Receta;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recetas")
@CrossOrigin(origins = "*")
public class RecetaController {

    @Autowired
    private RecetaRepository repositorio;

    @Autowired
    private MascotaRepository mascotaRepositorio;

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping("/mascota/{mascotaId}")
    public List<Receta> listarPorMascota(@PathVariable Integer mascotaId) {
        return repositorio.findAll().stream()
                .filter(r -> r.getMascota_id() != null && r.getMascota_id().equals(mascotaId))
                .toList();
    }

    @PostMapping
    public Receta guardar(@RequestBody Receta receta) { return repositorio.save(receta); }

    @PatchMapping("/{id}/estado")
    public Receta actualizarEstado(@PathVariable Integer id, @RequestBody Receta datos) {
        Receta existente = repositorio.findById(id).orElse(null);
        if (existente == null) return null;
        existente.setEstado(datos.getEstado());
        if (datos.getVeterinario_id() != null) existente.setVeterinario_id(datos.getVeterinario_id());
        if (datos.getContenido() != null) existente.setContenido(datos.getContenido());
        Receta guardada = repositorio.save(existente);

        Mascota mascota = mascotaRepositorio.findById(guardada.getMascota_id()).orElse(null);
        if (mascota != null && mascota.getUsuario_id() != null) {
            String mensaje = guardada.getEstado().equals("Aprobada")
                    ? "Tu receta médica fue aprobada por el veterinario."
                    : "Tu receta médica fue rechazada. Revisa el detalle en tu perfil o contáctanos.";
            notificacionService.notificar(mascota.getUsuario_id(), "Actualización de tu receta", mensaje, "Receta");
        }

        return guardada;
    }
}

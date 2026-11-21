package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.modelo.Receta;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.repositorio.RecetaRepository;
import com.vamospet.backend.servicio.NotificacionService;
import com.vamospet.backend.servicio.VentaService;
import com.vamospet.backend.permisos.Rol;
import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private VentaService ventaService;

    @GetMapping("/mascota/{mascotaId}")
    public List<Receta> listarPorMascota(@PathVariable Integer mascotaId) {
        return repositorio.findAll().stream()
                .filter(r -> r.getMascota_id() != null && r.getMascota_id().equals(mascotaId))
                .toList();
    }

    @GetMapping("/vigentes/cliente/{clienteId}")
    public List<Map<String, Object>> vigentes(@PathVariable Integer clienteId) {
        return ventaService.recetasUsables(clienteId);
    }

    @PostMapping
    public Receta guardar(@RequestBody Receta receta) {
        receta.setId(null);
        if (UsuarioActual.es(Rol.CLIENTE)) {
            boolean propia = receta.getMascota_id() != null && mascotaRepositorio.findById(receta.getMascota_id())
                    .map(m -> UsuarioActual.id().equals(m.getUsuario_id())).orElse(false);
            if (!propia) throw new NegocioException(403, "Solo puedes subir recetas de tus mascotas.");
            receta.setEstado("Pendiente");
            receta.setVeterinario_id(null);
        } else if (UsuarioActual.es(Rol.AUXILIAR)) {
            boolean conDueno = receta.getMascota_id() != null && mascotaRepositorio.findById(receta.getMascota_id())
                    .map(m -> m.getUsuario_id() != null).orElse(false);
            if (!conDueno) throw new NegocioException(400, "Elige la mascota del cliente a la que pertenece la receta.");
            if (receta.getArchivo_url() == null || receta.getArchivo_url().isBlank()) {
                throw new NegocioException(400, "Sube la foto de la receta en papel para que un veterinario la revise.");
            }
            receta.setEstado("Pendiente");
            receta.setVeterinario_id(null);
            if (receta.getContenido() == null || receta.getContenido().isBlank()) receta.setContenido("Receta en papel registrada en mostrador.");
        } else if (UsuarioActual.es(Rol.VETERINARIO)) {
            receta.setVeterinario_id(UsuarioActual.id());
            if ("Aprobada".equals(receta.getEstado())) receta.setAprobada_en(LocalDateTime.now());
        }
        receta.setUsada(false);
        receta.setUsada_en(null);
        receta.setPedido_id(null);
        return repositorio.save(receta);
    }

    @PatchMapping("/{id}/estado")
    public Receta actualizarEstado(@PathVariable Integer id, @RequestBody Receta datos) {
        Receta existente = repositorio.findById(id).orElse(null);
        if (existente == null) return null;
        if (Boolean.TRUE.equals(existente.getUsada())) throw new NegocioException(409, "Esta receta ya se usó en una compra y no se puede cambiar.");
        if (!"Aprobada".equals(datos.getEstado()) && !"Rechazada".equals(datos.getEstado())) {
            throw new NegocioException(400, "El estado debe ser Aprobada o Rechazada.");
        }
        if ("Aprobada".equals(datos.getEstado()) && !"Aprobada".equals(existente.getEstado())) existente.setAprobada_en(LocalDateTime.now());
        if ("Rechazada".equals(datos.getEstado())) existente.setAprobada_en(null);
        existente.setEstado(datos.getEstado());
        if (UsuarioActual.es(Rol.VETERINARIO)) existente.setVeterinario_id(UsuarioActual.id());
        else if (datos.getVeterinario_id() != null) existente.setVeterinario_id(datos.getVeterinario_id());
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

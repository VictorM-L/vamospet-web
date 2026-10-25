package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.repositorio.MascotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@CrossOrigin(origins = "*")
public class MascotaController {
    
    @Autowired
    private MascotaRepository repositorio;

    @GetMapping
    public List<Mascota> listarTodas() { 
        return repositorio.findAll(); 
    }

    @GetMapping("/{id}")
    public Mascota obtenerUna(@PathVariable Integer id) {
        return repositorio.findById(id).orElse(null);
    }

    @PostMapping
    public org.springframework.http.ResponseEntity<?> guardar(@RequestBody Mascota mascota) {
        if (mascota.getUsuario_id() == null) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "La mascota necesita un dueño. Para urgencias sin dueño usa el Ingreso por Emergencia."));
        }
        mascota.setCodigo_unico_temporal(null);
        if (mascota.getId() != null) {
            Mascota actual = repositorio.findById(mascota.getId()).orElse(null);
            if (actual != null && actual.getUsuario_id() != null && !actual.getUsuario_id().equals(mascota.getUsuario_id())) {
                return org.springframework.http.ResponseEntity.status(409)
                        .body(java.util.Map.of("error", "No se puede cambiar el dueño de una mascota ya registrada."));
            }
        }
        return org.springframework.http.ResponseEntity.ok(repositorio.save(mascota));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Mascota> obtenerMascotasPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.buscarPorDueno(usuarioId);
    }
}
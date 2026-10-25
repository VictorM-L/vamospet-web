package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Vacuna;
import com.vamospet.backend.repositorio.VacunaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vacunas")
@CrossOrigin(origins = "*")
public class VacunaController {

    @Autowired
    private VacunaRepository repositorio;

    @GetMapping("/mascota/{mascotaId}")
    public List<Vacuna> listarPorMascota(@PathVariable Integer mascotaId) {
        return repositorio.findAll().stream()
                .filter(v -> v.getMascota_id() != null && v.getMascota_id().equals(mascotaId))
                .toList();
    }

    @PostMapping
    public Vacuna guardar(@RequestBody Vacuna vacuna) { return repositorio.save(vacuna); }
}

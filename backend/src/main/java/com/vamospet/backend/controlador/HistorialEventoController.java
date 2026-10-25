package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.HistorialEvento;
import com.vamospet.backend.repositorio.HistorialEventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/historial")
@CrossOrigin(origins = "*")
public class HistorialEventoController {
    
    @Autowired
    private HistorialEventoRepository repositorio;

    @GetMapping("/mascota/{mascotaId}")
    public List<HistorialEvento> listarPorMascota(@PathVariable Integer mascotaId) {
        return repositorio.findAll().stream()
                .filter(e -> e.getMascota_id() != null && e.getMascota_id().equals(mascotaId))
                .toList();
    }

    @PostMapping
    public HistorialEvento guardar(@RequestBody HistorialEvento evento) { 
        return repositorio.save(evento); 
    }
}
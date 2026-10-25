package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Rol;
import com.vamospet.backend.repositorio.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RolController {
    
    @Autowired
    private RolRepository repositorio;

    @GetMapping
    public List<Rol> listar() { 
        return repositorio.findAll(); 
    }

    @PostMapping
    public Rol guardar(@RequestBody Rol rol) { 
        return repositorio.save(rol); 
    }
}
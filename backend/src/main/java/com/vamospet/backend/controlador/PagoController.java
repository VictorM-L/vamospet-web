package com.vamospet.backend.controlador;

import com.vamospet.backend.modelo.Pago;
import com.vamospet.backend.repositorio.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PagoController {

    @Autowired
    private PagoRepository repositorio;

    @GetMapping
    public List<Pago> listar() { return repositorio.findAll(); }

    @GetMapping("/usuario/{usuarioId}")
    public List<Pago> listarPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.findAll().stream()
                .filter(p -> p.getUsuario_id() != null && p.getUsuario_id().equals(usuarioId))
                .toList();
    }

    @PostMapping
    public Pago guardar(@RequestBody Pago pago) { return repositorio.save(pago); }
}

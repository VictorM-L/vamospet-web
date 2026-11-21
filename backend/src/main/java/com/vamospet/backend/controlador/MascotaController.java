package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.Mascota;
import com.vamospet.backend.repositorio.MascotaRepository;
import com.vamospet.backend.permisos.Rol;
import com.vamospet.backend.permisos.UsuarioActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@CrossOrigin(origins = "*")
public class MascotaController {
    
    public static final java.math.BigDecimal PESO_MAXIMO = new java.math.BigDecimal("200.00");

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
        if (UsuarioActual.es(Rol.CLIENTE)) mascota.setUsuario_id(UsuarioActual.id());
        if (mascota.getUsuario_id() == null) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "La mascota necesita un dueño. Para urgencias sin dueño usa el Ingreso por Emergencia."));
        }
        mascota.setCodigo_unico_temporal(null);
        Mascota actual = mascota.getId() == null ? null : repositorio.findById(mascota.getId()).orElse(null);
        if (actual != null && actual.getUsuario_id() != null && !actual.getUsuario_id().equals(mascota.getUsuario_id())) {
            return org.springframework.http.ResponseEntity.status(409)
                    .body(java.util.Map.of("error", "No se puede cambiar el dueño de una mascota ya registrada."));
        }
        mascota.setAlergias(actual != null ? actual.getAlergias() : null);
        mascota.setPeso_kg(actual != null ? actual.getPeso_kg() : null);
        return org.springframework.http.ResponseEntity.ok(repositorio.save(mascota));
    }

    @PatchMapping("/{id}/datos-clinicos")
    public org.springframework.http.ResponseEntity<?> datosClinicos(@PathVariable Integer id, @RequestBody java.util.Map<String, Object> b) {
        Mascota mascota = repositorio.findById(id).orElse(null);
        if (mascota == null) return org.springframework.http.ResponseEntity.notFound().build();
        Object a = b.get("alergias");
        String alergias = a == null || a.toString().isBlank() ? null : a.toString().trim();
        if (alergias != null && alergias.length() > 500) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "Las alergias admiten hasta 500 caracteres."));
        }
        java.math.BigDecimal peso = null;
        Object pv = b.get("peso_kg");
        if (pv != null && !pv.toString().isBlank()) {
            try {
                peso = new java.math.BigDecimal(pv.toString().trim()).setScale(2, java.math.RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "El peso no es un número válido."));
            }
            if (peso.signum() <= 0 || peso.compareTo(PESO_MAXIMO) > 0) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "El peso debe estar entre 0.01 y " + PESO_MAXIMO + " kg."));
            }
        }
        mascota.setAlergias(alergias);
        mascota.setPeso_kg(peso);
        return org.springframework.http.ResponseEntity.ok(repositorio.save(mascota));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Mascota> obtenerMascotasPorUsuario(@PathVariable Integer usuarioId) {
        return repositorio.buscarPorDueno(usuarioId);
    }
}
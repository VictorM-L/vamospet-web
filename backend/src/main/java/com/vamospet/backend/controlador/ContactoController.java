package com.vamospet.backend.controlador;

import com.vamospet.backend.dto.ContactoRequest;
import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.ContactoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contacto")
@CrossOrigin(origins = "*")
public class ContactoController {

    @Autowired
    private ContactoService servicio;

    @PostMapping
    public ResponseEntity<?> enviar(@Valid @RequestBody ContactoRequest datos, HttpServletRequest peticion) {
        servicio.recibir(datos, peticion.getRemoteAddr());
        return ResponseEntity.ok(Map.of("mensaje", "Recibimos tu mensaje. Te responderemos a tu correo."));
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(servicio.listar(UsuarioActual.id(), estado));
    }

    @PatchMapping("/{id}/responder")
    public ResponseEntity<?> responder(@PathVariable Integer id, @RequestBody Map<String, Object> b) {
        Object r = b.get("respuesta");
        return ResponseEntity.ok(servicio.responder(UsuarioActual.id(), id, r == null ? null : r.toString()));
    }

    @PatchMapping("/{id}/archivar")
    public ResponseEntity<?> archivar(@PathVariable Integer id) {
        return ResponseEntity.ok(servicio.archivar(UsuarioActual.id(), id));
    }
}

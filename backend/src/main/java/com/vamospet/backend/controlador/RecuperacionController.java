package com.vamospet.backend.controlador;

import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.seguridad.IntentosLoginService;
import com.vamospet.backend.dto.CodigoRequest;
import com.vamospet.backend.dto.CorreoRequest;
import com.vamospet.backend.dto.RestablecerRequest;
import com.vamospet.backend.servicio.RecuperacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class RecuperacionController {

    @Autowired
    private IntentosLoginService intentos;

    @Autowired
    private RecuperacionService recuperacion;

    @PostMapping("/recuperar/solicitar")
    public ResponseEntity<?> solicitar(@Valid @RequestBody CorreoRequest datos) {
        recuperacion.solicitar(datos.correo());
        return ResponseEntity.ok(Map.of("mensaje", RecuperacionService.MENSAJE_GENERICO));
    }

    @PostMapping("/recuperar/verificar")
    public ResponseEntity<?> verificar(@Valid @RequestBody CodigoRequest datos) {
        recuperacion.verificar(datos.correo(), datos.codigo());
        return ResponseEntity.ok(Map.of("valido", true));
    }

    @PostMapping("/recuperar/restablecer")
    public ResponseEntity<?> restablecer(@Valid @RequestBody RestablecerRequest datos) {
        recuperacion.restablecer(datos.correo(), datos.codigo(), datos.contrasena());
        intentos.limpiar(datos.correo());
        return ResponseEntity.ok(Map.of("mensaje", "Listo. Ya puedes iniciar sesión con tu nueva contraseña."));
    }

    @PostMapping("/personal/{id}/restablecimiento")
    public ResponseEntity<?> enviarRestablecimiento(@PathVariable Integer id) {
        Integer usuarioId = UsuarioActual.id();
        recuperacion.enviarRestablecimientoPorAdmin(usuarioId, id);
        return ResponseEntity.ok(Map.of("mensaje", "Enviamos el código de restablecimiento al correo del colaborador."));
    }
}

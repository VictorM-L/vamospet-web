package com.vamospet.backend.controlador;
import com.vamospet.backend.modelo.CodigoVerificacion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.CodigoVerificacionRepository;
import com.vamospet.backend.servicio.NotificacionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {
    
    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private CodigoVerificacionRepository codigoRepositorio;

    @Autowired
    private NotificacionService notificacionService;

    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${seguridad.bcrypt.activo:false}")
    private boolean bcryptActivo;

    private String codificar(String clave) {
        return bcryptActivo ? encoder.encode(clave) : clave;
    }

    private static final java.util.Set<Integer> ROLES_INTERNOS = java.util.Set.of(1, 3, 4);

    private boolean esPersonalInterno(Integer usuarioId) {
        if (usuarioId == null) return false;
        return repositorio.findById(usuarioId)
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .map(u -> ROLES_INTERNOS.contains(u.getRol_id()))
                .orElse(false);
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Integer usuarioId) {
        if (!esPersonalInterno(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para ver la lista de usuarios."));
        }
        return ResponseEntity.ok(repositorio.findAll());
    }

    @GetMapping("/buscar-dni/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni,
                                          @RequestParam(required = false) Integer usuarioId) {
        if (!esPersonalInterno(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin permiso."));
        }
        Usuario encontrado = repositorio.findByDni(dni);
        if (encontrado == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(encontrado);
    }

    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody Usuario credenciales) {
        String correo = credenciales.getCorreo();
        String clave = credenciales.getContrasena();
        if (correo == null || correo.isBlank() || clave == null || clave.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Correo o contraseña incorrectos."));
        }

        for (Usuario u : repositorio.findAllByCorreoIgnoreCase(correo.trim())) {
            if (!claveCoincide(u, clave)) continue;
            if (Boolean.FALSE.equals(u.getActivo())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Esta cuenta fue dada de baja. Comunícate con la veterinaria."));
            }
            return ResponseEntity.ok(u);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Correo o contraseña incorrectos."));
    }

    private boolean claveCoincide(Usuario u, String claveIngresada) {
        String guardada = u.getContrasena();
        if (guardada == null || guardada.isBlank()) return false;
        if (guardada.startsWith("$2")) return encoder.matches(claveIngresada, guardada);
        if (guardada.equals(claveIngresada)) {
            if (bcryptActivo) {
                u.setContrasena(encoder.encode(claveIngresada));
                repositorio.save(u);
            }
            return true;
        }
        return false;
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        String dni = StringUtils.trimToEmpty(nuevoUsuario.getDni());
        String correo = StringUtils.trimToEmpty(nuevoUsuario.getCorreo());
        String contrasena = nuevoUsuario.getContrasena();
        String nombre = StringUtils.normalizeSpace(nuevoUsuario.getNombre());

        if (StringUtils.isBlank(dni)) {
            return ResponseEntity.badRequest().body(Map.of("error", "El DNI es obligatorio."));
        }
        if (dni.length() != 8 || !StringUtils.isNumeric(dni)) {
            return ResponseEntity.badRequest().body(Map.of("error", "El DNI debe tener exactamente 8 dígitos."));
        }
        if (StringUtils.isAnyBlank(correo, contrasena)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo y contraseña son obligatorios."));
        }
        if (!CORREO_VALIDO.matcher(correo).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo no tiene un formato válido."));
        }
        if (StringUtils.length(contrasena) < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 8 caracteres."));
        }
        nuevoUsuario.setDni(dni);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setNombre(nombre);

        Usuario existentePorDni = repositorio.findByDni(nuevoUsuario.getDni());

        if (existentePorDni != null) {
            if (existentePorDni.getCorreo() != null && !existentePorDni.getCorreo().isBlank()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ese DNI ya tiene una cuenta registrada. Intenta iniciar sesión."));
            }
            if (correoEnUso(nuevoUsuario.getCorreo(), existentePorDni.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
            }
            String contacto = existentePorDni.getCorreo_contacto();
            if (contacto == null || contacto.isBlank()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                        "Ese DNI ya está registrado en la clínica sin cuenta web. Acércate al personal para activar tu cuenta."));
            }
            LocalDateTime ahora = LocalDateTime.now();
            boolean reciente = codigoRepositorio.findAll().stream().anyMatch(c ->
                    existentePorDni.getId().equals(c.getUsuario_id()) && c.getCreado() != null
                    && c.getCreado().isAfter(ahora.minusSeconds(60)));
            if (reciente) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error",
                        "Espera un minuto antes de pedir otro código."));
            }
            String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
            CodigoVerificacion cv = new CodigoVerificacion();
            cv.setUsuario_id(existentePorDni.getId());
            cv.setCodigo_hash(hashCodigo(existentePorDni.getId(), codigo));
            cv.setExpira(ahora.plusMinutes(10));
            codigoRepositorio.save(cv);
            boolean enviado = notificacionService.enviarCorreoDirecto(contacto, "Código de verificación",
                    "Tu código para crear tu cuenta es: " + codigo + "\nVence en 10 minutos. Si no fuiste tú, ignora este mensaje.");
            if (!enviado) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error",
                        "No pudimos enviar el código por correo. Inténtalo más tarde o acércate al personal."));
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "requiereCodigo", true,
                    "correoEnmascarado", enmascararCorreo(contacto),
                    "mensaje", "Enviamos un código de 6 dígitos a " + enmascararCorreo(contacto) + "."));
        }

        if (correoEnUso(nuevoUsuario.getCorreo(), null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }
        nuevoUsuario.setContrasena(codificar(nuevoUsuario.getContrasena()));
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setRol_id(2);
        return ResponseEntity.ok(repositorio.save(nuevoUsuario));
    }

    @PostMapping("/registro/confirmar")
    public ResponseEntity<?> confirmarRegistro(@RequestBody Map<String, String> b) {
        String dni = b.get("dni");
        String codigo = b.get("codigo");
        String correo = b.get("correo");
        String contrasena = b.get("contrasena");
        if (dni == null || codigo == null || correo == null || correo.isBlank() || contrasena == null || contrasena.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos."));
        }
        Usuario u = repositorio.findByDni(dni);
        if (u == null || (u.getCorreo() != null && !u.getCorreo().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No hay una verificación pendiente para ese DNI."));
        }
        CodigoVerificacion cv = codigoRepositorio.findAll().stream()
                .filter(c -> u.getId().equals(c.getUsuario_id()) && !Boolean.TRUE.equals(c.getUsado()))
                .max(Comparator.comparing(CodigoVerificacion::getCreado)).orElse(null);
        if (cv == null || cv.getExpira().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "El código venció. Pide uno nuevo."));
        }
        if (cv.getIntentos() >= 5) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "Demasiados intentos. Pide un código nuevo."));
        }
        if (!cv.getCodigo_hash().equals(hashCodigo(u.getId(), codigo.trim()))) {
            cv.setIntentos(cv.getIntentos() + 1);
            codigoRepositorio.save(cv);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Código incorrecto."));
        }
        if (correoEnUso(correo, u.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }
        cv.setUsado(true);
        codigoRepositorio.save(cv);
        u.setCorreo(correo.trim());
        u.setContrasena(codificar(contrasena));
        if (b.get("nombre") != null && !b.get("nombre").isBlank()) u.setNombre(b.get("nombre"));
        if (b.get("telefono") != null && !b.get("telefono").isBlank()) u.setTelefono(b.get("telefono"));
        if (b.get("direccion") != null) u.setDireccion(b.get("direccion"));
        u.setRol_id(2);
        return ResponseEntity.ok(repositorio.save(u));
    }

    @PatchMapping("/{id}/correo-contacto")
    public ResponseEntity<?> actualizarCorreoContacto(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Usuario u = repositorio.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        String c = body.get("correo_contacto");
        if (c == null || !CORREO_VALIDO.matcher(c.trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo no válido."));
        }
        u.setCorreo_contacto(c.trim());
        return ResponseEntity.ok(repositorio.save(u));
    }

    private String hashCodigo(Integer usuarioId, String codigo) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest((usuarioId + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String enmascararCorreo(String c) {
        int at = c.indexOf('@');
        if (at <= 1) return c;
        return c.charAt(0) + "***" + c.substring(at);
    }

    @PostMapping("/cliente-sin-cuenta")
    public ResponseEntity<?> crearClienteSinCuenta(@RequestBody Usuario datos) {
        if (datos.getDni() == null || datos.getDni().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El DNI es obligatorio."));
        }
        if (repositorio.findByDni(datos.getDni()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe un cliente con ese DNI."));
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(datos.getNombre());
        nuevo.setDni(datos.getDni());
        nuevo.setTelefono(datos.getTelefono());
        String cc = datos.getCorreo_contacto();
        if (cc != null && !cc.isBlank()) {
            if (!CORREO_VALIDO.matcher(cc.trim()).matches()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El correo de contacto no es válido."));
            }
            nuevo.setCorreo_contacto(cc.trim());
        }
        nuevo.setRol_id(2);
        return ResponseEntity.ok(repositorio.save(nuevo));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activarCuenta(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        if (usuario.getCorreo() != null && !usuario.getCorreo().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Este cliente ya tiene una cuenta activa."));
        }

        String correo = body.get("correo");
        String contrasena = body.get("contrasena");
        if (correo == null || correo.isBlank() || contrasena == null || contrasena.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo y contraseña son obligatorios."));
        }

        if (correoEnUso(correo, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }
        usuario.setCorreo(correo.trim());
        usuario.setContrasena(codificar(contrasena));
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    private boolean correoEnUso(String correo, Integer idExcluido) {
        if (correo == null || correo.isBlank()) return false;
        return repositorio.findAll().stream().anyMatch(u ->
                u.getCorreo() != null
                && u.getCorreo().equalsIgnoreCase(correo.trim())
                && (idExcluido == null || !idExcluido.equals(u.getId())));
    }

    @PostMapping("/personal")
    public ResponseEntity<?> crearPersonal(@RequestBody Map<String, Object> body) {
        String nombre = body.get("nombre") != null ? body.get("nombre").toString().trim() : "";
        String dni = body.get("dni") != null ? body.get("dni").toString().trim() : "";
        String correo = body.get("correo") != null ? body.get("correo").toString().trim() : "";
        String contrasena = body.get("contrasena") != null ? body.get("contrasena").toString() : "";
        String telefono = body.get("telefono") != null ? body.get("telefono").toString().trim() : "";

        Integer rolId;
        try {
            rolId = Integer.valueOf(body.get("rol_id").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol inválido."));
        }

        if (rolId != 3 && rolId != 4) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se puede crear personal con rol Veterinario (3) o Auxiliar de Veterinaria (4)."));
        }
        if (nombre.isBlank() || dni.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre, DNI, correo y contraseña son obligatorios."));
        }

        Usuario porDni = repositorio.findByDni(dni);
        if (correoEnUso(correo, porDni != null ? porDni.getId() : null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }

        if (porDni != null) {
            if (porDni.getCorreo() != null && !porDni.getCorreo().isBlank()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe una cuenta con ese DNI."));
            }
            porDni.setNombre(nombre);
            porDni.setCorreo(correo);
            porDni.setContrasena(codificar(contrasena));
            if (!telefono.isBlank()) porDni.setTelefono(telefono);
            porDni.setRol_id(rolId);
            return ResponseEntity.ok(repositorio.save(porDni));
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setDni(dni);
        nuevo.setCorreo(correo);
        nuevo.setContrasena(codificar(contrasena));
        nuevo.setTelefono(telefono);
        nuevo.setRol_id(rolId);
        return ResponseEntity.ok(repositorio.save(nuevo));
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        Integer nuevoRol = body.get("rol_id");
        if (nuevoRol == null) return ResponseEntity.badRequest().body(Map.of("error", "Falta rol_id."));
        usuario.setRol_id(nuevoRol);
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        if (Integer.valueOf(1).equals(usuario.getRol_id())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede dar de baja a un Administrador."));
        }
        usuario.setActivo(false);
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        usuario.setActivo(true);
        return ResponseEntity.ok(repositorio.save(usuario));
    }
}

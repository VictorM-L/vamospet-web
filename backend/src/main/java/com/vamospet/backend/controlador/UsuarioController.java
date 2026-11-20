package com.vamospet.backend.controlador;
import com.vamospet.backend.servicio.NegocioException;
import com.vamospet.backend.servicio.CitaService;
import com.vamospet.backend.repositorio.CitaRepository;
import com.vamospet.backend.dto.ConfirmarRegistroRequest;
import com.vamospet.backend.dto.PerfilRequest;
import com.vamospet.backend.dto.PersonalRequest;
import com.vamospet.backend.dto.RegistroRequest;
import com.vamospet.backend.modelo.CodigoVerificacion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.dto.RespuestaSesion;
import com.vamospet.backend.seguridad.IntentosLoginService;
import com.vamospet.backend.seguridad.SesionService;
import java.time.Duration;
import com.vamospet.backend.repositorio.UsuarioRepository;
import com.vamospet.backend.permisos.Rol;
import com.vamospet.backend.permisos.UsuarioActual;
import com.vamospet.backend.servicio.ClaveService;
import com.vamospet.backend.servicio.CodigoService;
import com.vamospet.backend.servicio.NotificacionService;
import com.vamospet.backend.servicio.RecuperacionService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {
    
    @Autowired
    private SesionService sesiones;

    @Autowired
    private IntentosLoginService intentos;

    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private ClaveService claves;

    @Autowired
    private CodigoService codigos;

    @Autowired
    private RecuperacionService recuperacion;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CitaRepository citaRepositorio;

    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final java.util.Set<Integer> ROLES_INTERNOS = java.util.Set.of(1, 3, 4);

    private boolean esPersonalInterno(Integer usuarioId) {
        if (usuarioId == null) return false;
        return repositorio.findById(usuarioId)
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .map(u -> ROLES_INTERNOS.contains(u.getRol_id()))
                .orElse(false);
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        Integer usuarioId = UsuarioActual.id();
        if (!esPersonalInterno(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para ver la lista de usuarios."));
        }
        return ResponseEntity.ok(repositorio.findAll());
    }

    @GetMapping("/buscar-dni/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni) {
        Integer usuarioId = UsuarioActual.id();
        if (!esPersonalInterno(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin permiso."));
        }
        Usuario encontrado = repositorio.findByDni(dni);
        if (encontrado == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(encontrado);
    }

    @GetMapping("/existe")
    public ResponseEntity<?> existe(@RequestParam(required = false) String dni,
                                    @RequestParam(required = false) String correo) {
        boolean hayDni = StringUtils.isNotBlank(dni);
        boolean hayCorreo = StringUtils.isNotBlank(correo);
        if (hayDni == hayCorreo) {
            return ResponseEntity.badRequest().body(Map.of("error", "Envía solo el DNI o solo el correo."));
        }
        if (hayDni) {
            Usuario u = repositorio.findByDni(dni.trim());
            boolean conCuenta = u != null && StringUtils.isNotBlank(u.getCorreo());
            return ResponseEntity.ok(Map.of("existe", conCuenta));
        }
        return ResponseEntity.ok(Map.of("existe", correoEnUso(correo, null)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody Usuario credenciales) {
        String correo = credenciales.getCorreo();
        String clave = credenciales.getContrasena();
        if (correo == null || correo.isBlank() || clave == null || clave.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Correo o contraseña incorrectos."));
        }

        Duration bloqueo = intentos.bloqueoRestante(correo);
        if (bloqueo != null) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", IntentosLoginService.mensajeBloqueo(bloqueo)));
        }

        for (Usuario u : repositorio.findAllByCorreoIgnoreCase(correo.trim())) {
            if (!claves.coincide(u, clave)) continue;
            if (Boolean.FALSE.equals(u.getActivo())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Esta cuenta fue dada de baja. Comunícate con la veterinaria."));
            }
            intentos.limpiar(correo);
            SesionService.Sesion sesion = sesiones.iniciar(u);
            return ResponseEntity.ok(new RespuestaSesion(u, sesion.token(), sesion.expira()));
        }
        int quedan = intentos.registrarFallo(correo);
        if (quedan == 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", IntentosLoginService.mensajeBloqueo(IntentosLoginService.BLOQUEO)));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", IntentosLoginService.mensajeFallo(quedan)));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody RegistroRequest datos) {
        String dni = datos.dni().trim();
        String correo = datos.correo().trim();
        String nombre = StringUtils.normalizeSpace(datos.nombre());

        Usuario existentePorDni = repositorio.findByDni(dni);

        if (existentePorDni != null) {
            if (StringUtils.isNotBlank(existentePorDni.getCorreo())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ese DNI ya tiene una cuenta registrada. Intenta iniciar sesión."));
            }
            if (correoEnUso(correo, existentePorDni.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
            }
            String contacto = existentePorDni.getCorreo_contacto();
            if (contacto == null || contacto.isBlank()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                        "Ese DNI ya está registrado en la clínica sin cuenta web. Acércate al personal para activar tu cuenta."));
            }
            LocalDateTime ahora = LocalDateTime.now();
            if (codigos.enviadosDesde(existentePorDni.getId(), CodigoService.REGISTRO, ahora.minusSeconds(60)) > 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error",
                        "Espera un minuto antes de pedir otro código."));
            }
            String codigo = codigos.generar(existentePorDni.getId(), CodigoService.REGISTRO, 10);
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

        if (correoEnUso(correo, null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }
        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setDni(dni);
        nuevo.setCorreo(correo);
        nuevo.setTelefono(datos.telefono().trim());
        nuevo.setContrasena(claves.codificar(datos.contrasena()));
        nuevo.setActivo(true);
        nuevo.setRol_id(2);
        return ResponseEntity.ok(repositorio.save(nuevo));
    }

    @PostMapping("/registro/confirmar")
    public ResponseEntity<?> confirmarRegistro(@Valid @RequestBody ConfirmarRegistroRequest datos) {
        Usuario u = repositorio.findByDni(datos.dni().trim());
        if (u == null || StringUtils.isNotBlank(u.getCorreo())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No hay una verificación pendiente para ese DNI."));
        }
        CodigoVerificacion cv = codigos.validar(u.getId(), CodigoService.REGISTRO, datos.codigo());
        if (correoEnUso(datos.correo(), u.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }
        codigos.marcarUsado(cv);
        u.setCorreo(datos.correo().trim());
        u.setContrasena(claves.codificar(datos.contrasena()));
        u.setNombre(StringUtils.normalizeSpace(datos.nombre()));
        u.setTelefono(datos.telefono().trim());
        u.setRol_id(2);
        u.setActivo(true);
        return ResponseEntity.ok(repositorio.save(u));
    }

    @PutMapping("/{id}/perfil")
    public ResponseEntity<?> actualizarPerfil(@PathVariable Integer id,
                                              @Valid @RequestBody PerfilRequest datos) {
        Integer usuarioId = UsuarioActual.id();
        if (usuarioId == null || !usuarioId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puedes editar tu propio perfil."));
        }
        Usuario u = repositorio.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if (Boolean.FALSE.equals(u.getActivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Esta cuenta fue dada de baja."));
        }
        u.setNombre(StringUtils.normalizeSpace(datos.nombre()));
        u.setTelefono(datos.telefono().trim());
        u.setDireccion(StringUtils.trimToNull(datos.direccion()));
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

    @PostMapping("/{id}/invitacion")
    public ResponseEntity<?> invitarACrearCuenta(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Usuario cliente = repositorio.findById(id).orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();
        if (!Integer.valueOf(2).equals(cliente.getRol_id())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Solo se invita a clientes."));
        }
        if (StringUtils.isNotBlank(cliente.getCorreo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Este cliente ya tiene una cuenta activa."));
        }
        String correo = body.get("correo") == null ? cliente.getCorreo_contacto() : body.get("correo").toString().trim();
        if (StringUtils.isBlank(correo) || !CORREO_VALIDO.matcher(correo).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Escribe un correo válido del cliente."));
        }
        if (correoEnUso(correo, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado en otra cuenta."));
        }
        cliente.setCorreo_contacto(correo);
        repositorio.save(cliente);
        boolean enviado = notificacionService.enviarCorreoDirecto(correo, "Crea tu cuenta en Vamos Pet",
                "Hola " + StringUtils.defaultString(cliente.getNombre()) + ".\n"
                        + "La clínica Vamos Pet te invita a crear tu cuenta web para ver el historial de tus mascotas, agendar citas y comprar en la tienda.\n\n"
                        + "Entra a la web, elige \"Crear cuenta\" y registra tu DNI " + StringUtils.defaultString(cliente.getDni())
                        + " con este mismo correo. Te enviaremos un código de 6 dígitos para que crees tu propia contraseña.\n"
                        + "Nadie de la clínica conoce ni elige tu contraseña.");
        if (!enviado) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error",
                    "Guardamos el correo, pero no pudimos enviar la invitación. Inténtalo más tarde."));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Enviamos la invitación a " + enmascararCorreo(correo) + "."));
    }

    private boolean correoEnUso(String correo, Integer idExcluido) {
        if (correo == null || correo.isBlank()) return false;
        return repositorio.findAll().stream().anyMatch(u ->
                u.getCorreo() != null
                && u.getCorreo().equalsIgnoreCase(correo.trim())
                && (idExcluido == null || !idExcluido.equals(u.getId())));
    }

    @PostMapping("/personal")
    public ResponseEntity<?> crearPersonal(@Valid @RequestBody PersonalRequest datos) {
        String nombre = StringUtils.normalizeSpace(datos.nombre());
        String dni = datos.dni().trim();
        String correo = datos.correo().trim();
        String telefono = StringUtils.trimToEmpty(datos.telefono());
        Integer rolId = datos.rol_id();

        if (rolId != 3 && rolId != 4) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se puede crear personal con rol Veterinario (3) o Auxiliar de Veterinaria (4)."));
        }

        Usuario porDni = repositorio.findByDni(dni);
        if (correoEnUso(correo, porDni != null ? porDni.getId() : null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ese correo ya está registrado."));
        }

        Usuario colaborador;
        if (porDni != null) {
            if (StringUtils.isNotBlank(porDni.getCorreo())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe una cuenta con ese DNI."));
            }
            colaborador = porDni;
        } else {
            colaborador = new Usuario();
            colaborador.setDni(dni);
        }
        colaborador.setNombre(nombre);
        colaborador.setCorreo(correo);
        colaborador.setContrasena(null);
        if (!telefono.isBlank()) colaborador.setTelefono(telefono);
        colaborador.setRol_id(rolId);
        colaborador.setActivo(true);
        Usuario guardado = repositorio.save(colaborador);
        boolean correoEnviado = recuperacion.enviarBienvenidaPersonal(guardado);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("usuario", guardado);
        respuesta.put("correoEnviado", correoEnviado);
        return ResponseEntity.ok(respuesta);
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

    private ResponseEntity<?> noPuedeGestionar(Usuario objetivo) {
        boolean esPersonal = objetivo.getRol_id() != null && ROLES_INTERNOS.contains(objetivo.getRol_id());
        if (esPersonal && !UsuarioActual.es(Rol.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo el Administrador gestiona al personal."));
        }
        return null;
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        if (Integer.valueOf(1).equals(usuario.getRol_id())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede dar de baja a un Administrador."));
        }
        ResponseEntity<?> prohibido = noPuedeGestionar(usuario);
        if (prohibido != null) return prohibido;
        if (Integer.valueOf(3).equals(usuario.getRol_id())) {
            long citasPorAtender = citaRepositorio.findAll().stream()
                    .filter(c -> id.equals(c.getVeterinario_id()) && CitaService.ACTIVAS.contains(c.getEstado())
                            && c.getFecha_hora() != null && c.getFecha_hora().isAfter(LocalDateTime.now()))
                    .count();
            if (citasPorAtender > 0) {
                throw new NegocioException(409, "No se puede dar de baja: el veterinario tiene " + citasPorAtender
                        + " cita(s) por atender. Pídele que marque \"No puedo atender\" para reasignarlas primero.");
            }
        }
        usuario.setActivo(false);
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        ResponseEntity<?> prohibido = noPuedeGestionar(usuario);
        if (prohibido != null) return prohibido;
        usuario.setActivo(true);
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    @PatchMapping("/{id}/grooming")
    public ResponseEntity<?> grooming(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        if (!Integer.valueOf(3).equals(usuario.getRol_id())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo un veterinario puede atender grooming."));
        }
        Object valor = body.get("hace_grooming");
        if (valor == null) return ResponseEntity.badRequest().body(Map.of("error", "Indica si atiende grooming."));
        usuario.setHace_grooming(Boolean.parseBoolean(valor.toString()));
        return ResponseEntity.ok(repositorio.save(usuario));
    }
}

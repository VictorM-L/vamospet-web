package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.CodigoVerificacion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
public class RecuperacionService {

    public static final int VIGENCIA_MINUTOS = 15;
    public static final int MAX_ENVIOS = 3;
    public static final int VENTANA_MINUTOS = 15;
    public static final String MENSAJE_GENERICO =
            "Si el correo está registrado, te enviamos un código de 6 dígitos. Revisa tu bandeja de entrada y la carpeta de spam.";

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);
    private static final Set<Integer> ROLES_PERSONAL = Set.of(AutorizacionService.ADMIN,
            AutorizacionService.VETERINARIO, AutorizacionService.AUXILIAR);

    @Autowired
    private UsuarioRepository usuarioRepositorio;

    @Autowired
    private CodigoService codigos;

    @Autowired
    private ClaveService claves;

    @Autowired
    private NotificacionService notificaciones;

    @Autowired
    private AutorizacionService autorizacion;

    public void solicitar(String correo) {
        Optional<Usuario> cuenta = cuentaActiva(correo);
        if (cuenta.isEmpty()) {
            log.info("Recuperación pedida para un correo sin cuenta activa");
            return;
        }
        Usuario u = cuenta.get();
        if (limiteAlcanzado(u)) {
            log.info("Recuperación sin envío: el usuario {} llegó al límite de códigos", u.getId());
            return;
        }
        enviarCodigo(u, "Código para restablecer tu contraseña",
                "Recibimos una solicitud para restablecer la contraseña de tu cuenta.");
    }

    public void verificar(String correo, String codigo) {
        Usuario u = cuentaActiva(correo).orElseThrow(RecuperacionService::codigoIncorrecto);
        codigos.validar(u.getId(), CodigoService.RECUPERACION, codigo);
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public void restablecer(String correo, String codigo, String nuevaContrasena) {
        Usuario u = cuentaActiva(correo).orElseThrow(RecuperacionService::codigoIncorrecto);
        CodigoVerificacion c = codigos.validar(u.getId(), CodigoService.RECUPERACION, codigo);
        codigos.marcarUsado(c);
        u.setContrasena(claves.codificar(nuevaContrasena));
        usuarioRepositorio.save(u);
        notificaciones.enviarCorreoDirecto(u.getCorreo(), "Tu contraseña fue cambiada",
                "Hola " + nombreCorto(u) + ", la contraseña de tu cuenta se cambió el "
                        + LocalDateTime.now().withNano(0).toString().replace('T', ' ') + ".\n"
                        + "Si no fuiste tú, comunícate de inmediato con la clínica.");
    }

    public void enviarRestablecimientoPorAdmin(Integer adminId, Integer colaboradorId) {
        if (autorizacion.conRol(adminId, AutorizacionService.ADMIN) == null) {
            throw new NegocioException(403, "Solo el administrador puede enviar un restablecimiento.");
        }
        Usuario colaborador = usuarioRepositorio.findById(colaboradorId)
                .orElseThrow(() -> new NegocioException(404, "No encontramos a ese colaborador."));
        if (colaborador.getRol_id() == null || !ROLES_PERSONAL.contains(colaborador.getRol_id())) {
            throw new NegocioException(409, "Solo se envía restablecimiento al personal de la clínica.");
        }
        if (Boolean.FALSE.equals(colaborador.getActivo())) {
            throw new NegocioException(409, "Ese colaborador está de baja. Reactívalo primero.");
        }
        if (colaborador.getCorreo() == null || colaborador.getCorreo().isBlank()) {
            throw new NegocioException(409, "Ese colaborador no tiene correo registrado.");
        }
        if (limiteAlcanzado(colaborador)) {
            throw new NegocioException(429, "Ya se enviaron " + MAX_ENVIOS + " códigos en los últimos "
                    + VENTANA_MINUTOS + " minutos. Espera un momento.");
        }
        boolean enviado = enviarCodigo(colaborador, "Restablece tu contraseña",
                "El administrador de la clínica pidió que restablezcas tu contraseña.");
        if (!enviado) {
            throw new NegocioException(503, "No pudimos enviar el correo. Inténtalo más tarde.");
        }
    }

    public boolean enviarBienvenidaPersonal(Usuario colaborador) {
        if (colaborador.getCorreo() == null || colaborador.getCorreo().isBlank()) return false;
        return enviarCodigo(colaborador, "Crea tu contraseña",
                "Se creó tu cuenta de personal en Vamos Pet. Para entrar por primera vez debes crear tu contraseña.");
    }

    private boolean enviarCodigo(Usuario u, String asunto, String introduccion) {
        String codigo = codigos.generar(u.getId(), CodigoService.RECUPERACION, VIGENCIA_MINUTOS);
        return notificaciones.enviarCorreoDirecto(u.getCorreo(), asunto,
                "Hola " + nombreCorto(u) + ".\n" + introduccion + "\n\n"
                        + "Tu código es: " + codigo + "\n"
                        + "Vence en " + VIGENCIA_MINUTOS + " minutos y solo sirve una vez.\n\n"
                        + "Entra a la web de Vamos Pet, elige \"¿Olvidaste tu contraseña?\" y luego \"Ya tengo un código\".\n"
                        + "Si el código venció, pide uno nuevo desde esa misma pantalla.\n"
                        + "Si no pediste este cambio, ignora este mensaje.");
    }

    private boolean limiteAlcanzado(Usuario u) {
        LocalDateTime desde = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        return codigos.enviadosDesde(u.getId(), CodigoService.RECUPERACION, desde) >= MAX_ENVIOS;
    }

    private Optional<Usuario> cuentaActiva(String correo) {
        if (correo == null || correo.isBlank()) return Optional.empty();
        return usuarioRepositorio.findAllByCorreoIgnoreCase(correo.trim()).stream()
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .findFirst();
    }

    private static String nombreCorto(Usuario u) {
        String n = u.getNombre() == null ? "" : u.getNombre().trim();
        return n.isEmpty() ? "" : n.split("\\s+")[0];
    }

    private static NegocioException codigoIncorrecto() {
        return new NegocioException(400, "Código incorrecto o vencido.");
    }
}

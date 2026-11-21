package com.vamospet.backend.servicio;

import com.vamospet.backend.dto.ContactoRequest;
import com.vamospet.backend.modelo.MensajeContacto;
import com.vamospet.backend.repositorio.MensajeContactoRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class ContactoService {

    public static final String NUEVO = "Nuevo";
    public static final String RESPONDIDO = "Respondido";
    public static final String ARCHIVADO = "Archivado";
    public static final Set<String> ESTADOS = Set.of(NUEVO, RESPONDIDO, ARCHIVADO);
    public static final int MAXIMO_POR_IP_HORA = 5;
    public static final int MAXIMO_POR_CORREO_HORA = 3;
    public static final int LARGO_MAXIMO = 2000;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired private MensajeContactoRepository repositorio;
    @Autowired private UsuarioRepository usuarioRepositorio;
    @Autowired private AutorizacionService autorizacion;
    @Autowired private NotificacionService notificaciones;

    @Transactional
    public MensajeContacto recibir(ContactoRequest r, String ip) {
        MensajeContacto m = new MensajeContacto();
        m.setNombre(r.nombre().trim());
        m.setCorreo(r.correo().trim().toLowerCase());
        m.setTelefono(r.telefono() == null || r.telefono().isBlank() ? null : r.telefono().trim());
        m.setAsunto(r.asunto().trim());
        m.setMensaje(r.mensaje().trim());
        m.setIp_origen(ip == null ? null : ip.length() > 45 ? ip.substring(0, 45) : ip);
        if (r.sitio_web() != null && !r.sitio_web().isBlank()) return m;
        if (m.getMensaje().length() > LARGO_MAXIMO) throw new NegocioException(400, "El mensaje no puede pasar de " + LARGO_MAXIMO + " caracteres.");
        LocalDateTime haceUnaHora = LocalDateTime.now().minusHours(1);
        if (m.getIp_origen() != null && repositorio.contarPorIpDesde(m.getIp_origen(), haceUnaHora) >= MAXIMO_POR_IP_HORA) {
            throw new NegocioException(429, "Recibimos varios mensajes desde tu conexión en la última hora. Intenta más tarde o escríbenos por WhatsApp.");
        }
        if (repositorio.contarPorCorreoDesde(m.getCorreo(), haceUnaHora) >= MAXIMO_POR_CORREO_HORA) {
            throw new NegocioException(429, "Ya recibimos tus mensajes de la última hora. Te responderemos pronto.");
        }
        MensajeContacto guardado = repositorio.save(m);
        usuarioRepositorio.findAll().stream()
                .filter(u -> Integer.valueOf(AutorizacionService.AUXILIAR).equals(u.getRol_id()) && !Boolean.FALSE.equals(u.getActivo()))
                .forEach(u -> notificaciones.notificar(u.getId(), "Nuevo mensaje de contacto",
                        guardado.getNombre() + " escribió sobre: " + guardado.getAsunto() + ". Respóndelo desde la bandeja de contacto.", "Contacto"));
        return guardado;
    }

    public List<MensajeContacto> listar(Integer actorId, String estado) {
        if (autorizacion.conRol(actorId, AutorizacionService.ADMIN, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "No tienes permiso para ver los mensajes de contacto.");
        }
        if (estado != null && !estado.isBlank() && !ESTADOS.contains(estado)) throw new NegocioException(400, "El estado no es válido.");
        return repositorio.recientes().stream().filter(m -> estado == null || estado.isBlank() || estado.equals(m.getEstado())).toList();
    }

    public static String textoRespuesta(MensajeContacto m, String respuesta) {
        StringBuilder sb = new StringBuilder("Hola ").append(m.getNombre()).append(",\n\n").append(respuesta.trim())
                .append("\n\nEl ").append(m.getCreado_en() == null ? "" : m.getCreado_en().format(FORMATO))
                .append(", ").append(m.getNombre()).append(" escribió:\n");
        for (String linea : m.getMensaje().split("\\r?\\n", -1)) sb.append("> ").append(linea).append('\n');
        return sb.toString().stripTrailing();
    }

    public static String asuntoRespuesta(MensajeContacto m) {
        return "Re: " + m.getAsunto();
    }

    @Transactional
    public MensajeContacto responder(Integer actorId, Integer id, String respuesta) {
        exigirAuxiliar(actorId);
        MensajeContacto m = obtener(id);
        if (!NUEVO.equals(m.getEstado())) throw new NegocioException(409, "Este mensaje ya está " + m.getEstado().toLowerCase() + ".");
        String r = respuesta == null ? "" : respuesta.trim();
        if (r.length() < 5) throw new NegocioException(400, "Escribe la respuesta (al menos 5 caracteres).");
        if (r.length() > LARGO_MAXIMO) throw new NegocioException(400, "La respuesta no puede pasar de " + LARGO_MAXIMO + " caracteres.");
        if (!notificaciones.enviarCorreo(m.getCorreo(), asuntoRespuesta(m), textoRespuesta(m, r), null, null)) {
            throw new NegocioException(502, "No se pudo enviar el correo. La respuesta no se guardó; inténtalo de nuevo.");
        }
        m.setRespuesta(r);
        m.setEstado(RESPONDIDO);
        m.setRespondido_por(actorId);
        m.setRespondido_en(LocalDateTime.now());
        return repositorio.save(m);
    }

    @Transactional
    public MensajeContacto archivar(Integer actorId, Integer id) {
        exigirAuxiliar(actorId);
        MensajeContacto m = obtener(id);
        if (ARCHIVADO.equals(m.getEstado())) throw new NegocioException(409, "Este mensaje ya está archivado.");
        m.setEstado(ARCHIVADO);
        return repositorio.save(m);
    }

    private void exigirAuxiliar(Integer actorId) {
        if (autorizacion.conRol(actorId, AutorizacionService.AUXILIAR) == null) {
            throw new NegocioException(403, "Solo el Auxiliar responde o archiva los mensajes de contacto.");
        }
    }

    private MensajeContacto obtener(Integer id) {
        if (id == null) throw new NegocioException(404, "El mensaje no existe.");
        return repositorio.findById(id).orElseThrow(() -> new NegocioException(404, "El mensaje no existe."));
    }
}

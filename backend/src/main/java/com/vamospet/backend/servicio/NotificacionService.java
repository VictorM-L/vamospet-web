package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Notificacion;
import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.NotificacionRepository;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    @Autowired
    private NotificacionRepository notificacionRepositorio;

    @Autowired
    private UsuarioRepository usuarioRepositorio;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private List<CanalNotificacion> canales;

    public Notificacion notificar(Integer usuarioId, String titulo, String mensaje, String tipo) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario_id(usuarioId);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo);
        notificacion.setEnviado(true);
        notificacion.setFecha_envio(LocalDateTime.now());
        Notificacion guardada = notificacionRepositorio.save(notificacion);
        entregar(usuarioId, titulo, mensaje);
        return guardada;
    }

    public Notificacion programar(Integer usuarioId, String titulo, String mensaje, String tipo,
                                  LocalDateTime cuando, String referencia) {
        Notificacion n = new Notificacion();
        n.setUsuario_id(usuarioId);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setEnviado(false);
        n.setFecha_programada(cuando);
        n.setReferencia(referencia);
        return notificacionRepositorio.save(n);
    }

    public void cancelarProgramadas(String referencia) {
        List<Notificacion> pendientes = notificacionRepositorio.findAll().stream()
                .filter(n -> Boolean.FALSE.equals(n.getEnviado()) && referencia.equals(n.getReferencia()))
                .toList();
        notificacionRepositorio.deleteAll(pendientes);
    }

    public int procesarPendientes() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Notificacion> vencidas = notificacionRepositorio.findAll().stream()
                .filter(n -> Boolean.FALSE.equals(n.getEnviado()) && n.getFecha_programada() != null
                        && !n.getFecha_programada().isAfter(ahora))
                .toList();
        for (Notificacion n : vencidas) {
            n.setEnviado(true);
            n.setFecha_envio(ahora);
            n.setFecha(ahora);
            notificacionRepositorio.save(n);
            entregar(n.getUsuario_id(), n.getTitulo(), n.getMensaje());
        }
        return vencidas.size();
    }

    private void entregar(Integer usuarioId, String titulo, String mensaje) {
        Usuario usuario = usuarioRepositorio.findById(usuarioId).orElse(null);
        if (usuario == null) return;
        for (CanalNotificacion canal : canales) {
            try {
                canal.enviar(usuario, titulo, mensaje);
            } catch (Exception e) {
                log.warn("No se pudo entregar por {}: {}", canal.nombre(), e.getMessage());
            }
        }
    }

    public boolean enviarCorreoDirecto(String destino, String asunto, String texto) {
        try {
            SimpleMailMessage correo = new SimpleMailMessage();
            correo.setFrom("vamospetnotis@gmail.com");
            correo.setTo(destino);
            correo.setSubject("Vamos Pet — " + asunto);
            correo.setText(texto + "\n\n— Equipo Vamos Pet Clínica Veterinaria");
            mailSender.send(correo);
            return true;
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo a {}: {}", destino, e.getMessage());
            return false;
        }
    }
}

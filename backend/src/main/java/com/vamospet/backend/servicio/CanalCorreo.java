package com.vamospet.backend.servicio;

import com.vamospet.backend.modelo.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class CanalCorreo implements CanalNotificacion {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public String nombre() { return "correo"; }

    @Override
    public void enviar(Usuario destinatario, String titulo, String mensaje) {
        if (destinatario.getCorreo() == null || destinatario.getCorreo().isBlank()) return;
        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom("vamospetnotis@gmail.com");
        correo.setTo(destinatario.getCorreo());
        correo.setSubject("Vamos Pet — " + titulo);
        correo.setText(mensaje + "\n\n— Equipo Vamos Pet Clínica Veterinaria");
        mailSender.send(correo);
    }
}

package com.vamospet.backend.config;

import com.vamospet.backend.modelo.Usuario;
import com.vamospet.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class MigracionContrasenas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionContrasenas.class);

    @Autowired
    private UsuarioRepository usuarioRepositorio;

    @Autowired
    private PasswordEncoder encoder;

    @Value("${seguridad.bcrypt.activo:false}")
    private boolean bcryptActivo;

    @Override
    public void run(ApplicationArguments args) {
        if (!bcryptActivo) return;
        int migradas = 0;
        for (Usuario u : usuarioRepositorio.findAll()) {
            String c = u.getContrasena();
            if (c != null && !c.isBlank() && !c.startsWith("$2")) {
                u.setContrasena(encoder.encode(c));
                usuarioRepositorio.save(u);
                migradas++;
            }
        }
        if (migradas > 0) {
            log.info("[Seguridad] Contraseñas migradas a BCrypt: {}", migradas);
        }
    }
}

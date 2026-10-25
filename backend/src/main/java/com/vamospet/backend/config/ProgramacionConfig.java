package com.vamospet.backend.config;

import com.vamospet.backend.servicio.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ProgramacionConfig {

    private static final Logger log = LoggerFactory.getLogger(ProgramacionConfig.class);

    @Autowired
    private NotificacionService notificacionService;

    @Scheduled(fixedDelay = 60000, initialDelay = 20000)
    public void enviarNotificacionesProgramadas() {
        try {
            notificacionService.procesarPendientes();
        } catch (Exception e) {
            log.error("Error en el envío de notificaciones programadas", e);
        }
    }
}

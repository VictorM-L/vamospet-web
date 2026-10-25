package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
}

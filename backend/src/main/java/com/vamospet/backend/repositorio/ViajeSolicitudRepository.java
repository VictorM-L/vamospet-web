package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.ViajeSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViajeSolicitudRepository extends JpaRepository<ViajeSolicitud, Integer> {}

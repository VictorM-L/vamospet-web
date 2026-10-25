package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Integer> {
}

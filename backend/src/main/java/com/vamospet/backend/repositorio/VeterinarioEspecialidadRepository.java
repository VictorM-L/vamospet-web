package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.VeterinarioEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeterinarioEspecialidadRepository extends JpaRepository<VeterinarioEspecialidad, Integer> {
}

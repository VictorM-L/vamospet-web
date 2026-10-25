package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Integer> {
}

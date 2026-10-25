package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.DevolucionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DevolucionDetalleRepository extends JpaRepository<DevolucionDetalle, Integer> {
}

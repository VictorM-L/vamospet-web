package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Integer> {
}

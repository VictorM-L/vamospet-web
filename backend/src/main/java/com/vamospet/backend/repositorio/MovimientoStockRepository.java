package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Integer> {
}

package com.vamospet.backend.repositorio;
import com.vamospet.backend.modelo.HistorialEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialEventoRepository extends JpaRepository<HistorialEvento, Integer> {}
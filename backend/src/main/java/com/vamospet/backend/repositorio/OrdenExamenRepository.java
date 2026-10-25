package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.OrdenExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenExamenRepository extends JpaRepository<OrdenExamen, Integer> {}

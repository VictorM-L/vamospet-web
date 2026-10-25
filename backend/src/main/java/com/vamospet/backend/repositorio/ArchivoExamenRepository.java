package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.ArchivoExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchivoExamenRepository extends JpaRepository<ArchivoExamen, Integer> {}

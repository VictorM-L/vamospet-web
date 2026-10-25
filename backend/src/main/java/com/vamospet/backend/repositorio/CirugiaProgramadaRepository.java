package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.CirugiaProgramada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CirugiaProgramadaRepository extends JpaRepository<CirugiaProgramada, Integer> {}

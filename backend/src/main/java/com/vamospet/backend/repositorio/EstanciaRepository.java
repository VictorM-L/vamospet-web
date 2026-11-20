package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.Estancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstanciaRepository extends JpaRepository<Estancia, Integer> {}

package com.vamospet.backend.repositorio;
import com.vamospet.backend.modelo.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Integer> {}
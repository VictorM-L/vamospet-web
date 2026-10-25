package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Integer> {
    
    @Query(value = "SELECT * FROM mascota WHERE usuario_id = :id", nativeQuery = true)
    List<Mascota> buscarPorDueno(@Param("id") Integer id);
}
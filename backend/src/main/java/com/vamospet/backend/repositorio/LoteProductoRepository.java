package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.LoteProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Integer> {

    @Query("select l from LoteProducto l where l.producto_id = :productoId")
    List<LoteProducto> delProducto(@Param("productoId") Integer productoId);
}

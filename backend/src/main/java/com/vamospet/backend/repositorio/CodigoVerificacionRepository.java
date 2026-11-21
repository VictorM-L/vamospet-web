package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Integer> {

    @Query("select c from CodigoVerificacion c where c.usuario_id = :usuarioId and c.proposito = :proposito order by c.creado desc, c.id desc")
    List<CodigoVerificacion> buscarRecientes(@Param("usuarioId") Integer usuarioId, @Param("proposito") String proposito);

    @Query("select count(c) from CodigoVerificacion c where c.usuario_id = :usuarioId and c.proposito = :proposito and c.creado > :desde")
    long contarDesde(@Param("usuarioId") Integer usuarioId, @Param("proposito") String proposito, @Param("desde") LocalDateTime desde);
}

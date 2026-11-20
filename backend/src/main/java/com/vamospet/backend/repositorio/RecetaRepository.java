package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Receta r set r.usada = true, r.usada_en = :ahora, r.pedido_id = :pedidoId "
            + "where r.id = :id and r.estado = 'Aprobada' and (r.usada is null or r.usada = false)")
    int marcarUsada(@Param("id") Integer id, @Param("pedidoId") Integer pedidoId, @Param("ahora") LocalDateTime ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Receta r set r.usada = false, r.usada_en = null, r.pedido_id = null where r.pedido_id = :pedidoId")
    int liberarDePedido(@Param("pedidoId") Integer pedidoId);
}

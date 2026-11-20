package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.ViajeDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViajeDocumentoRepository extends JpaRepository<ViajeDocumento, Integer> {

    @Query("select d from ViajeDocumento d where d.viaje_id = :viajeId order by d.subido_en desc, d.id desc")
    List<ViajeDocumento> deViaje(@Param("viajeId") Integer viajeId);
}

package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.MensajeContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Integer> {

    @Query("select count(m) from MensajeContacto m where m.ip_origen = :ip and m.creado_en > :desde")
    long contarPorIpDesde(@Param("ip") String ip, @Param("desde") LocalDateTime desde);

    @Query("select count(m) from MensajeContacto m where lower(m.correo) = lower(:correo) and m.creado_en > :desde")
    long contarPorCorreoDesde(@Param("correo") String correo, @Param("desde") LocalDateTime desde);

    @Query("select m from MensajeContacto m order by m.creado_en desc, m.id desc")
    List<MensajeContacto> recientes();
}

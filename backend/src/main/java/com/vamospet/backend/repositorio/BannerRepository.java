package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {

    @Query("select b from Banner b where b.seccion = :seccion order by b.orden asc, b.id asc")
    List<Banner> deSeccion(@Param("seccion") String seccion);
}

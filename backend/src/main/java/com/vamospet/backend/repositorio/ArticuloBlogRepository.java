package com.vamospet.backend.repositorio;
import com.vamospet.backend.modelo.ArticuloBlog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticuloBlogRepository extends JpaRepository<ArticuloBlog, Integer> {}
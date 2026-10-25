package com.vamospet.backend.repositorio;

import com.vamospet.backend.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findAllByCorreoIgnoreCase(String correo);
    Usuario findByDni(String dni);
}

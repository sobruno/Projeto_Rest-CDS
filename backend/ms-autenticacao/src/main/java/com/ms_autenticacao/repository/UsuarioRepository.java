package com.ms_autenticacao.repository;

import com.ms_autenticacao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNome(String nome);
    Optional<Usuario> findByNomeIgnoreCase(String nome);
    Optional<Usuario> findByEmailIgnoreCase(String email);
}

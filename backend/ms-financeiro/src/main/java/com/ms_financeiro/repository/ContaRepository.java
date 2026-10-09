package com.ms_financeiro.repository;

import com.ms_financeiro.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    Optional<Conta> findByUsuarioId(Long usuarioId);
}

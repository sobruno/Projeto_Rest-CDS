package com.ms_financeiro.repository;

import com.ms_financeiro.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(Long origem, Long destino);
}

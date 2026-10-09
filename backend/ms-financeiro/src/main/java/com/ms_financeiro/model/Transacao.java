package com.ms_financeiro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long contaOrigemId;
    private Long contaDestinoId;
    private Double valor;
    private LocalDateTime dataHora;
    private String tipo;

    public Transacao() {}

    public Transacao(Long contaOrigemId, Long contaDestinoId, Double valor, LocalDateTime dataHora, String tipo) {
        this.contaOrigemId = contaOrigemId;
        this.contaDestinoId = contaDestinoId;
        this.valor = valor;
        this.dataHora = dataHora;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getContaOrigemId() { return contaOrigemId; }
    public void setContaOrigemId(Long contaOrigemId) { this.contaOrigemId = contaOrigemId; }
    public Long getContaDestinoId() { return contaDestinoId; }
    public void setContaDestinoId(Long contaDestinoId) { this.contaDestinoId = contaDestinoId; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}

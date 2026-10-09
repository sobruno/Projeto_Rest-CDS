package com.ms_financeiro.model;

import jakarta.persistence.*;

@Entity
public class Conta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long usuarioId;
    private String titular;
    private String chave;
    private Double saldo;

    public Conta() {}

    public Conta(Long usuarioId, String titular, String chave, Double saldo) {
        this.usuarioId = usuarioId;
        this.titular = titular;
        this.chave = chave;
        this.saldo = saldo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
}

package com.ms_financeiro.dto;

public class ContaDto {
    private Long id;
    private Long usuarioId;
    private String titular;
    private Double saldo;

    public ContaDto() {}
    public ContaDto(Long id, Long usuarioId, String titular, Double saldo) {
        this.id = id; this.usuarioId = usuarioId; this.titular = titular; this.saldo = saldo;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
}

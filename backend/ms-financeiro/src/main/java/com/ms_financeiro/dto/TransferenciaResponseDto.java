package com.ms_financeiro.dto;

public class TransferenciaResponseDto extends ContaDto {

    private Long destinatarioUsuarioId;
    private Long destinatarioContaId;
    private String destinatarioTitular;
    private String destinatarioChave;
    private Double destinatarioSaldo;

    private Long transacaoId;
    private Double valor;
    private String dataHora;

    public TransferenciaResponseDto() {
        super();
    }

    public TransferenciaResponseDto(Long id, Long usuarioId, String titular, String chave, Double saldo) {
        super(id, usuarioId, titular, chave, saldo);
    }

    public Long getDestinatarioUsuarioId() {
        return destinatarioUsuarioId;
    }

    public void setDestinatarioUsuarioId(Long destinatarioUsuarioId) {
        this.destinatarioUsuarioId = destinatarioUsuarioId;
    }

    public Long getDestinatarioContaId() {
        return destinatarioContaId;
    }

    public void setDestinatarioContaId(Long destinatarioContaId) {
        this.destinatarioContaId = destinatarioContaId;
    }

    public String getDestinatarioTitular() {
        return destinatarioTitular;
    }

    public void setDestinatarioTitular(String destinatarioTitular) {
        this.destinatarioTitular = destinatarioTitular;
    }

    public String getDestinatarioChave() {
        return destinatarioChave;
    }

    public void setDestinatarioChave(String destinatarioChave) {
        this.destinatarioChave = destinatarioChave;
    }

    public Double getDestinatarioSaldo() {
        return destinatarioSaldo;
    }

    public void setDestinatarioSaldo(Double destinatarioSaldo) {
        this.destinatarioSaldo = destinatarioSaldo;
    }

    public Long getTransacaoId() {
        return transacaoId;
    }

    public void setTransacaoId(Long transacaoId) {
        this.transacaoId = transacaoId;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }
}

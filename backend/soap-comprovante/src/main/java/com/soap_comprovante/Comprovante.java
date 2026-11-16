package com.soap_comprovante;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Comprovante", namespace = "http://soap.banco.com/")
public class Comprovante implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(required = true)
    private String codigoComprovante;

    @XmlElement(required = true)
    private String autenticacaoEletronica;

    @XmlElement(required = true)
    private String nomeRemetente;

    @XmlElement(required = true)
    private String nomeDestinatario;

    @XmlElement(required = true)
    private String chaveDestinatario;

    @XmlElement(required = true)
    private Double valor;

    @XmlElement(required = true)
    private String dataHora;

    @XmlElement(required = true)
    private String status;

    public Comprovante() {
    }

    public Comprovante(String codigoComprovante, String autenticacaoEletronica, String nomeRemetente,
                       String nomeDestinatario, String chaveDestinatario, Double valor,
                       String dataHora, String status) {
        this.codigoComprovante = codigoComprovante;
        this.autenticacaoEletronica = autenticacaoEletronica;
        this.nomeRemetente = nomeRemetente;
        this.nomeDestinatario = nomeDestinatario;
        this.chaveDestinatario = chaveDestinatario;
        this.valor = valor;
        this.dataHora = dataHora;
        this.status = status;
    }

    public String getCodigoComprovante() {
        return codigoComprovante;
    }

    public void setCodigoComprovante(String codigoComprovante) {
        this.codigoComprovante = codigoComprovante;
    }

    public String getAutenticacaoEletronica() {
        return autenticacaoEletronica;
    }

    public void setAutenticacaoEletronica(String autenticacaoEletronica) {
        this.autenticacaoEletronica = autenticacaoEletronica;
    }

    public String getNomeRemetente() {
        return nomeRemetente;
    }

    public void setNomeRemetente(String nomeRemetente) {
        this.nomeRemetente = nomeRemetente;
    }

    public String getNomeDestinatario() {
        return nomeDestinatario;
    }

    public void setNomeDestinatario(String nomeDestinatario) {
        this.nomeDestinatario = nomeDestinatario;
    }

    public String getChaveDestinatario() {
        return chaveDestinatario;
    }

    public void setChaveDestinatario(String chaveDestinatario) {
        this.chaveDestinatario = chaveDestinatario;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

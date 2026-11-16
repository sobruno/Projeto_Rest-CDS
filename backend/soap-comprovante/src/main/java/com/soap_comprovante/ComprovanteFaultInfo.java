package com.soap_comprovante;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComprovanteFaultInfo", namespace = "http://soap.banco.com/")
public class ComprovanteFaultInfo {

    @XmlElement(required = true)
    private String codigoErro;

    @XmlElement(required = true)
    private String mensagem;

    public ComprovanteFaultInfo() {
    }

    public ComprovanteFaultInfo(String codigoErro, String mensagem) {
        this.codigoErro = codigoErro;
        this.mensagem = mensagem;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

    public void setCodigoErro(String codigoErro) {
        this.codigoErro = codigoErro;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}

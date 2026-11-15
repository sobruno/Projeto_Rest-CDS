package com.soap_comprovante;

import jakarta.xml.ws.WebFault;

@WebFault(name = "ComprovanteFault", targetNamespace = "http://soap.banco.com/")
public class ComprovanteFault extends Exception {

    private static final long serialVersionUID = 1L;

    private ComprovanteFaultInfo faultInfo;

    public ComprovanteFault(String message, ComprovanteFaultInfo faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public ComprovanteFault(String message, ComprovanteFaultInfo faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public ComprovanteFaultInfo getFaultInfo() {
        return faultInfo;
    }
}

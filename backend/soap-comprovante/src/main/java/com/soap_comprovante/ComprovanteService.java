package com.soap_comprovante;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(targetNamespace = "http://soap.banco.com/", name = "ComprovanteService")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface ComprovanteService {

    @WebMethod(operationName = "gerarComprovante")
    @WebResult(name = "comprovante")
    Comprovante gerarComprovante(
            @WebParam(name = "nomeRemetente") String nomeRemetente,
            @WebParam(name = "nomeDestinatario") String nomeDestinatario,
            @WebParam(name = "chaveDestinatario") String chaveDestinatario,
            @WebParam(name = "valor") Double valor,
            @WebParam(name = "dataHora") String dataHora
    ) throws ComprovanteFault;

    @WebMethod(operationName = "consultarComprovante")
    @WebResult(name = "comprovante")
    Comprovante consultarComprovante(
            @WebParam(name = "codigoComprovante") String codigoComprovante
    ) throws ComprovanteFault;
}

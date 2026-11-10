package com.soap_comprovante;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * Contrato WSDL do Serviço SOAP de Comprovantes Bancários.
 * Expõe as operações obrigatórias: emissão e consulta/validação.
 */
@WebService(targetNamespace = "http://soap.banco.com/", name = "ComprovanteService")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface ComprovanteService {

    /**
     * Operação 1: Emite um comprovante oficial para uma transferência realizada.
     */
    @WebMethod(operationName = "gerarComprovante")
    @WebResult(name = "comprovante")
    Comprovante gerarComprovante(
            @WebParam(name = "nomeRemetente") String nomeRemetente,
            @WebParam(name = "nomeDestinatario") String nomeDestinatario,
            @WebParam(name = "chaveDestinatario") String chaveDestinatario,
            @WebParam(name = "valor") Double valor,
            @WebParam(name = "dataHora") String dataHora
    ) throws ComprovanteFault;

    /**
     * Operação 2: Consulta um comprovante já emitido pelo seu código identificador.
     */
    @WebMethod(operationName = "consultarComprovante")
    @WebResult(name = "comprovante")
    Comprovante consultarComprovante(
            @WebParam(name = "codigoComprovante") String codigoComprovante
    ) throws ComprovanteFault;
}

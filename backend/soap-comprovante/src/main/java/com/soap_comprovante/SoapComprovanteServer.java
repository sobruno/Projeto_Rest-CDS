package com.soap_comprovante;

import jakarta.xml.ws.Endpoint;

public class SoapComprovanteServer {

    public static final String URL_ENDPOINT = "http://0.0.0.0:8088/ws/comprovante";

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("      INICIANDO SERVIÇO WEB SOAP - COMPROVANTES BANCÁRIOS      ");
        System.out.println("===============================================================");

        ComprovanteServiceImpl service = new ComprovanteServiceImpl();
        Endpoint endpoint = Endpoint.publish(URL_ENDPOINT, service);

        System.out.println("[SOAP-SERVER] Endpoint publicado com sucesso!");
        System.out.println("[SOAP-SERVER] URL do Serviço : " + URL_ENDPOINT);
        System.out.println("[SOAP-SERVER] WSDL disponível: " + URL_ENDPOINT + "?wsdl");
        System.out.println("[SOAP-SERVER] Pressione CTRL+C para encerrar o servidor.");
        System.out.println("===============================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SOAP-SERVER] Encerrando endpoint SOAP...");
            if (endpoint.isPublished()) {
                endpoint.stop();
            }
            System.out.println("[SOAP-SERVER] Servidor finalizado.");
        }));
    }
}

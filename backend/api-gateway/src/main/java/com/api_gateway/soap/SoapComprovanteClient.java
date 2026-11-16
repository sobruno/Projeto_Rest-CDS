package com.api_gateway.soap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class SoapComprovanteClient {

    @Value("${SOAP_COMPROVANTE_URL:${soap.comprovante.url:http://localhost:8088/ws/comprovante}}")
    private String soapEndpointUrl;

    public Map<String, Object> gerarComprovante(String nomeRemetente, String nomeDestinatario,
                                                String chaveDestinatario, Double valor,
                                                String dataHora) {
        System.out.println("[GATEWAY -> SOAP] Invocando servico SOAP em: " + soapEndpointUrl);

        String soapRequest = String.format("""
                <soapenv:Envelope xmlns:soapenv="http:
                   <soapenv:Header/>
                   <soapenv:Body>
                      <ws:gerarComprovante>
                         <nomeRemetente>%s</nomeRemetente>
                         <nomeDestinatario>%s</nomeDestinatario>
                         <chaveDestinatario>%s</chaveDestinatario>
                         <valor>%.2f</valor>
                         <dataHora>%s</dataHora>
                      </ws:gerarComprovante>
                   </soapenv:Body>
                </soapenv:Envelope>
                """,
                escapeXml(nomeRemetente != null ? nomeRemetente : "Usuario"),
                escapeXml(nomeDestinatario != null ? nomeDestinatario : "Destinatario"),
                escapeXml(chaveDestinatario != null ? chaveDestinatario : ""),
                valor != null ? valor : 0.0,
                escapeXml(dataHora != null ? dataHora : "")
        );

        try {
            URL url = new URL(soapEndpointUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "\"\"");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapRequest.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            byte[] bytes = responseStream.readAllBytes();
            String responseXml = new String(bytes, StandardCharsets.UTF_8);

            if (responseCode >= 200 && responseCode < 300) {
                return parseComprovanteXml(responseXml);
            } else {
                System.err.println("[GATEWAY -> SOAP] Resposta com erro HTTP " + responseCode + ": " + responseXml);
                Map<String, Object> errMap = new HashMap<>();
                errMap.put("status", "ERRO_SOAP");
                errMap.put("detalhe", responseXml);
                return errMap;
            }
        } catch (Exception e) {
            System.err.println("[GATEWAY -> SOAP] Falha na comunicacao com servico SOAP: " + e.getMessage());
            Map<String, Object> errMap = new HashMap<>();
            errMap.put("status", "SOAP_INDISPONIVEL");
            errMap.put("mensagem", e.getMessage());
            return errMap;
        }
    }

    private Map<String, Object> parseComprovanteXml(String xml) {
        Map<String, Object> result = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            result.put("codigoComprovante", getTagValue(doc, "codigoComprovante"));
            result.put("autenticacaoEletronica", getTagValue(doc, "autenticacaoEletronica"));
            result.put("status", getTagValue(doc, "status"));
            result.put("nomeRemetente", getTagValue(doc, "nomeRemetente"));
            result.put("nomeDestinatario", getTagValue(doc, "nomeDestinatario"));
            result.put("chaveDestinatario", getTagValue(doc, "chaveDestinatario"));
            result.put("valor", getTagValue(doc, "valor"));
            result.put("dataHora", getTagValue(doc, "dataHora"));
        } catch (Exception e) {
            result.put("rawXml", xml);
        }
        return result;
    }

    private String getTagValue(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagNameNS("*", tagName);
        if (list.getLength() == 0) {
            list = doc.getElementsByTagName(tagName);
        }
        if (list.getLength() > 0 && list.item(0).getFirstChild() != null) {
            return list.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

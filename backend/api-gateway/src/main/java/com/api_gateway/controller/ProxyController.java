package com.api_gateway.controller;

import com.api_gateway.security.JwtUtil;
import com.api_gateway.soap.SoapComprovanteClient;
import com.api_gateway.websocket.NotificationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProxyController {

    @Value("${MS_AUTENTICACAO_URL:${ms.autenticacao.url:http://localhost:8081}}")
    private String msAutenticacaoUrl;

    @Value("${MS_FINANCEIRO_URL:${ms.financeiro.url:http://localhost:8082}}")
    private String msFinanceiroUrl;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SoapComprovanteClient soapComprovanteClient;

    @Autowired
    private NotificationWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = createRestTemplate();

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        return new RestTemplate(requestFactory);
    }

    private static final List<String> IGNORED_HEADERS = List.of(
            "host", "content-length", "transfer-encoding", "connection",
            "keep-alive", "te", "trailers", "upgrade", "proxy-authorization", "proxy-authenticate",
            "access-control-allow-origin", "access-control-allow-methods",
            "access-control-allow-headers", "access-control-allow-credentials"
    );

    @GetMapping("/docs/{service}")
    public ResponseEntity<byte[]> openApiDocument(@PathVariable String service) {
        String serviceUrl = switch (service) {
            case "autenticacao" -> msAutenticacaoUrl;
            case "financeiro" -> msFinanceiroUrl;
            default -> null;
        };
        if (serviceUrl == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(serviceUrl + "/v3/api-docs", byte[].class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (Exception e) {
            byte[] error = ("{\"erro\":\"Nao foi possivel carregar a documentacao do servico " + service + "\"}")
                .getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().replaceFirst("^/api", "");
        String targetBaseUrl;

        if (path.startsWith("/auth")) {
            targetBaseUrl = msAutenticacaoUrl;
        } else {
            targetBaseUrl = msFinanceiroUrl;
        }

        String targetUrl = targetBaseUrl + path;
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        HttpHeaders headers = new HttpHeaders();
        var headerEnum = request.getHeaderNames();
        if (headerEnum != null) {
            Collections.list(headerEnum).forEach(headerName -> {
                if (!IGNORED_HEADERS.contains(headerName.toLowerCase())) {
                    headers.add(headerName, request.getHeader(headerName));
                }
            });
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                var claims = jwtUtil.parseClaims(token);
                headers.set("X-User-Id", claims.getSubject());
                if (claims.get("nome") != null) {
                    headers.set("X-User-Nome", claims.get("nome").toString());
                }
                if (claims.get("endereco") != null) {
                    headers.set("X-User-Chave", claims.get("endereco").toString());
                }
            } catch (Exception ignored) {}
        }

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(URI.create(targetUrl), httpMethod, httpEntity, byte[].class);
            byte[] respBody = response.getBody();

            if (path.equals("/contas/transferir") && httpMethod == HttpMethod.POST && response.getStatusCode().is2xxSuccessful() && respBody != null) {
                respBody = orquestrarTransferencia(respBody);
            }

            HttpHeaders cleanHeaders = filterResponseHeaders(response.getHeaders());
            var builder = ResponseEntity.status(response.getStatusCode()).headers(cleanHeaders);
            if (respBody != null) {
                builder.contentLength(respBody.length);
                return builder.body(respBody);
            }
            return builder.build();
        } catch (HttpStatusCodeException e) {
            byte[] errBody = e.getResponseBodyAsByteArray();
            HttpHeaders cleanHeaders = filterResponseHeaders(e.getResponseHeaders());

            var builder = ResponseEntity.status(e.getStatusCode()).headers(cleanHeaders);
            if (errBody != null && errBody.length > 0) {
                builder.contentLength(errBody.length);
                return builder.body(errBody);
            }
            return builder.build();
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            byte[] errBytes = ("{\"erro\":\"Erro ao comunicar com o serviço de destino: " + msg + "\"}").getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(errBytes.length)
                    .body(errBytes);
        }
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private byte[] orquestrarTransferencia(byte[] respBody) {
        String rawJson = new String(respBody, StandardCharsets.UTF_8);
        System.out.println("[GATEWAY] JSON de transferencia recebido do ms-financeiro: " + rawJson);
        try {
            Map<String, Object> map = objectMapper.readValue(rawJson, Map.class);
            if (map == null) return respBody;

            Map<String, Object> data;
            Object contentObj = map.get("content");
            if (contentObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> contentMap = (Map<String, Object>) contentObj;
                data = new HashMap<>(contentMap);

                map.forEach((k, v) -> { if (!k.equals("content")) data.putIfAbsent(k, v); });
            } else {

                data = new HashMap<>(map);
            }

            System.out.println("[GATEWAY] Dados aplanados para orquestracao: "
                    + "titular=" + data.get("titular")
                    + " valor=" + data.get("valor")
                    + " destUsuarioId=" + data.get("destinatarioUsuarioId"));

            String nomeRemetente    = data.get("titular") != null             ? data.get("titular").toString()             : "Remetente";
            String nomeDestinatario = data.get("destinatarioTitular") != null ? data.get("destinatarioTitular").toString() : "Destinatario";
            String chaveDestinatario= data.get("destinatarioChave") != null   ? data.get("destinatarioChave").toString()   : "";
            Double valor            = toDouble(data.get("valor"));
            String dataHora         = data.get("dataHora") != null            ? data.get("dataHora").toString()            : "";

            if (valor == null || valor <= 0.0) {
                System.err.println("[GATEWAY] Valor invalido extraido para comprovante: " + data.get("valor") + " — pulando SOAP.");
                valor = 0.01;
            }

            try {
                Map<String, Object> comprovante = soapComprovanteClient.gerarComprovante(
                        nomeRemetente, nomeDestinatario, chaveDestinatario, valor, dataHora);
                data.put("comprovante", comprovante);
                System.out.println("[GATEWAY] Comprovante SOAP gerado com sucesso.");
            } catch (Exception soapEx) {
                System.err.println("[GATEWAY] SOAP indisponivel: " + soapEx.getMessage());
                Map<String, Object> comprovanteErro = new HashMap<>();
                comprovanteErro.put("status", "SOAP_INDISPONIVEL");
                data.put("comprovante", comprovanteErro);
            }

            Long destUserId = toLong(data.get("destinatarioUsuarioId"));
            if (destUserId != null) {
                Double novoSaldoDestino = toDouble(data.get("destinatarioSaldo"));
                Map<String, Object> notifDestino = new HashMap<>();
                notifDestino.put("tipo", "SALDO_ATUALIZADO");
                notifDestino.put("saldo", novoSaldoDestino);
                Map<String, Object> tx = new HashMap<>();
                tx.put("tipo", "RECEBIDA"); tx.put("valor", valor);
                tx.put("de", nomeRemetente); tx.put("dataHora", dataHora);
                notifDestino.put("transacao", tx);
                webSocketHandler.enviarParaUsuario(destUserId, notifDestino);
                System.out.println("[GATEWAY] WebSocket enviado ao destinatario ID=" + destUserId);
            }

            Long origUserId = toLong(data.get("usuarioId"));
            if (origUserId != null) {
                Double novoSaldoOrigem = toDouble(data.get("saldo"));
                Map<String, Object> notifOrigem = new HashMap<>();
                notifOrigem.put("tipo", "SALDO_ATUALIZADO");
                notifOrigem.put("saldo", novoSaldoOrigem);
                Map<String, Object> tx = new HashMap<>();
                tx.put("tipo", "TRANSFERENCIA"); tx.put("valor", valor);
                tx.put("para", nomeDestinatario); tx.put("dataHora", dataHora);
                notifOrigem.put("transacao", tx);
                webSocketHandler.enviarParaUsuario(origUserId, notifOrigem);
                System.out.println("[GATEWAY] WebSocket enviado ao remetente ID=" + origUserId);
            }

            return objectMapper.writeValueAsBytes(data);

        } catch (Exception e) {
            System.err.println("[GATEWAY] Erro CRITICO na orquestracao pos-transferencia: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return respBody;
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders headers) {
        HttpHeaders cleanHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!IGNORED_HEADERS.contains(key.toLowerCase())) {
                    cleanHeaders.put(key, value);
                }
            });
        }
        return cleanHeaders;
    }
}

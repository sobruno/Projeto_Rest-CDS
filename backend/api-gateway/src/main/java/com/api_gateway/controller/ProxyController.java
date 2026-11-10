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

    // Cabeçalhos que NUNCA devem ser repassados entre Gateway <-> Microsserviços / Cliente
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

        // 1. Filtra os cabeçalhos de entrada enviando apenas os necessários
        HttpHeaders headers = new HttpHeaders();
        var headerEnum = request.getHeaderNames();
        if (headerEnum != null) {
            Collections.list(headerEnum).forEach(headerName -> {
                if (!IGNORED_HEADERS.contains(headerName.toLowerCase())) {
                    headers.add(headerName, request.getHeader(headerName));
                }
            });
        }

        // 2. Injeta dados do usuário extraídos do Token JWT para o microsserviço de destino
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

            // Orquestração: após conclusão da transferência, invoca SOAP e notifica WebSocket
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

    private byte[] orquestrarTransferencia(byte[] respBody) {
        try {
            Map<String, Object> map = objectMapper.readValue(respBody, Map.class);
            if (map != null) {
                String nomeRemetente = map.get("titular") != null ? map.get("titular").toString() : "Remetente";
                String nomeDestinatario = map.get("destinatarioTitular") != null ? map.get("destinatarioTitular").toString() : "Destinatario";
                String chaveDestinatario = map.get("destinatarioChave") != null ? map.get("destinatarioChave").toString() : "";
                Double valor = map.get("valor") != null ? Double.parseDouble(map.get("valor").toString()) : 0.0;
                String dataHora = map.get("dataHora") != null ? map.get("dataHora").toString() : "";

                // 1. Invoca serviço SOAP para emissão do comprovante
                Map<String, Object> comprovante = soapComprovanteClient.gerarComprovante(
                        nomeRemetente,
                        nomeDestinatario,
                        chaveDestinatario,
                        valor,
                        dataHora
                );
                map.put("comprovante", comprovante);

                // 2. Notifica o favorecido via WebSocket em tempo real
                if (map.get("destinatarioUsuarioId") != null) {
                    Long destUserId = Long.parseLong(map.get("destinatarioUsuarioId").toString());
                    Double novoSaldoDestino = map.get("destinatarioSaldo") != null
                            ? Double.parseDouble(map.get("destinatarioSaldo").toString()) : null;

                    Map<String, Object> notifDestino = new HashMap<>();
                    notifDestino.put("tipo", "SALDO_ATUALIZADO");
                    notifDestino.put("saldo", novoSaldoDestino);
                    Map<String, Object> transacaoDestino = new HashMap<>();
                    transacaoDestino.put("tipo", "RECEBIDA");
                    transacaoDestino.put("valor", valor);
                    transacaoDestino.put("de", nomeRemetente);
                    transacaoDestino.put("dataHora", dataHora);
                    notifDestino.put("transacao", transacaoDestino);

                    webSocketHandler.enviarParaUsuario(destUserId, notifDestino);
                }

                // 3. Notifica o remetente via WebSocket (outras abas/sessões abertas)
                if (map.get("usuarioId") != null) {
                    Long origUserId = Long.parseLong(map.get("usuarioId").toString());
                    Double novoSaldoOrigem = map.get("saldo") != null
                            ? Double.parseDouble(map.get("saldo").toString()) : null;

                    Map<String, Object> notifOrigem = new HashMap<>();
                    notifOrigem.put("tipo", "SALDO_ATUALIZADO");
                    notifOrigem.put("saldo", novoSaldoOrigem);
                    Map<String, Object> transacaoOrigem = new HashMap<>();
                    transacaoOrigem.put("tipo", "TRANSFERENCIA");
                    transacaoOrigem.put("valor", valor);
                    transacaoOrigem.put("para", nomeDestinatario);
                    transacaoOrigem.put("dataHora", dataHora);
                    notifOrigem.put("transacao", transacaoOrigem);

                    webSocketHandler.enviarParaUsuario(origUserId, notifOrigem);
                }

                return objectMapper.writeValueAsBytes(map);
            }
        } catch (Exception e) {
            System.err.println("[GATEWAY] Erro na orquestracao pos-transferencia: " + e.getMessage());
        }
        return respBody;
    }

    // 3. Filtra os cabeçalhos de resposta retornados pelos microsserviços
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

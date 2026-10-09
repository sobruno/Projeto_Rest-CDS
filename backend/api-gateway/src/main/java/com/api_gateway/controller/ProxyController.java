package com.api_gateway.controller;

import com.api_gateway.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
public class ProxyController {

    @Value("${MS_AUTENTICACAO_URL:${ms.autenticacao.url:http://ms-autenticacao:8081}}")
    private String msAutenticacaoUrl;

    @Value("${MS_FINANCEIRO_URL:${ms.financeiro.url:http://ms-financeiro:8082}}")
    private String msFinanceiroUrl;

    @Autowired
    private JwtUtil jwtUtil;

    private final RestTemplate restTemplate = new RestTemplate();

    // Cabeçalhos que NUNCA devem ser duplicados/repassados entre Gateway <-> Microsserviços
    private static final List<String> IGNORED_HEADERS = List.of(
            "host", "content-length", "transfer-encoding", "connection",
            "keep-alive", "te", "trailers", "upgrade", "proxy-authorization", "proxy-authenticate"
    );

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
            return ResponseEntity.status(response.getStatusCode())
                    .headers(filterResponseHeaders(response.getHeaders()))
                    .body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(filterResponseHeaders(e.getResponseHeaders()))
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"erro\":\"Erro ao comunicar com o serviço de destino: " + msg + "\"}").getBytes());
        }
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
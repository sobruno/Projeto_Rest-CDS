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

@RestController
public class ProxyController {

    @Value("${MS_AUTENTICACAO_URL:http://ms-autenticacao:8081}")
    private String msAutenticacaoUrl;

    @Value("${MS_FINANCEIRO_URL:http://ms-financeiro:8082}")
    private String msFinanceiroUrl;

    @Autowired
    private JwtUtil jwtUtil;

    private final RestTemplate restTemplate = new RestTemplate();

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().replace("/api", "");
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
                headers.add(headerName, request.getHeader(headerName));
            });
        }

        // Repassa/injeta o X-User-Id extraído do Token JWT para o microsserviço de destino
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String userId = jwtUtil.extrairUsuarioId(token);
                headers.set("X-User-Id", userId);
            } catch (Exception ignored) {}
        }

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());

        try {
            return restTemplate.exchange(URI.create(targetUrl), httpMethod, httpEntity, byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(("{\"erro\":\"Erro ao comunicar com o serviço de destino: " + msg + "\"}").getBytes());
        }
    }
}
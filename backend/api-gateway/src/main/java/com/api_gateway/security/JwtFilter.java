package com.api_gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Injeta os cabeçalhos de CORS em TODAS as respostas (sucesso, preflight ou erro)
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-User-Id, *");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 2. Responde preflight (OPTIONS) imediatamente com status 200 OK
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI();

        // 3. Libera rotas públicas de autenticação (/api/auth/**)
        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Validação do Token JWT e extração do ID do Usuário
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validarToken(token)) {
                String userId = jwtUtil.extrairUsuarioId(token);

                // Empacota o request injetando o cabeçalho X-User-Id
                HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return userId;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return Collections.enumeration(List.of(userId));
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> names = Collections.list(super.getHeaderNames());
                        if (!names.contains("X-User-Id")) {
                            names.add("X-User-Id");
                        }
                        return Collections.enumeration(names);
                    }
                };

                filterChain.doFilter(wrappedRequest, response);
                return;
            }
        }

        // 5. Retorna 401 para requisições sem token válido
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"erro\": \"Token invalido ou ausente\"}");
    }
}
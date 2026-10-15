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

        // 1. Responde ou delega preflight (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 2. Libera rotas públicas de autenticação (/api/auth/**)
        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Validação do Token JWT e extração do ID do Usuário
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validarToken(token)) {
                var claims = jwtUtil.parseClaims(token);
                String userId = claims.getSubject();
                String userNome = claims.get("nome") != null ? claims.get("nome").toString() : null;
                String userChave = claims.get("endereco") != null ? claims.get("endereco").toString() : null;

                // Empacota o request injetando os cabeçalhos do usuário
                HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return userId;
                        }
                        if ("X-User-Nome".equalsIgnoreCase(name) && userNome != null) {
                            return userNome;
                        }
                        if ("X-User-Chave".equalsIgnoreCase(name) && userChave != null) {
                            return userChave;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return Collections.enumeration(List.of(userId));
                        }
                        if ("X-User-Nome".equalsIgnoreCase(name) && userNome != null) {
                            return Collections.enumeration(List.of(userNome));
                        }
                        if ("X-User-Chave".equalsIgnoreCase(name) && userChave != null) {
                            return Collections.enumeration(List.of(userChave));
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> names = Collections.list(super.getHeaderNames());
                        if (!names.contains("X-User-Id")) names.add("X-User-Id");
                        if (userNome != null && !names.contains("X-User-Nome")) names.add("X-User-Nome");
                        if (userChave != null && !names.contains("X-User-Chave")) names.add("X-User-Chave");
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
package com.api_gateway.websocket;

import com.api_gateway.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long usuarioId = extrairUsuarioIdDaSessao(session);
        if (usuarioId != null) {
            associarUsuario(session, usuarioId);
            System.out.println("[WEBSOCKET] Sessão conectada e associada ao usuário ID: " + usuarioId);
        } else {
            System.out.println("[WEBSOCKET] Sessão conectada sem token imediato: " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {

            Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if (payload != null && "AUTH".equals(payload.get("tipo"))) {
                Object tokenObj = payload.get("token");
                if (tokenObj != null) {
                    var claims = jwtUtil.parseClaims(tokenObj.toString());
                    Long usuarioId = Long.parseLong(claims.getSubject());
                    associarUsuario(session, usuarioId);
                    session.sendMessage(new TextMessage("{\"tipo\":\"AUTH_OK\",\"mensagem\":\"Conectado com sucesso\"}"));
                    System.out.println("[WEBSOCKET] Usuário autenticado via mensagem: " + usuarioId);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long usuarioId = sessionToUser.remove(session.getId());
        if (usuarioId != null) {
            Set<WebSocketSession> sessions = userSessions.get(usuarioId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(usuarioId);
                }
            }
        }
        System.out.println("[WEBSOCKET] Sessão encerrada: " + session.getId());
    }

    private void associarUsuario(WebSocketSession session, Long usuarioId) {
        sessionToUser.put(session.getId(), usuarioId);
        userSessions.computeIfAbsent(usuarioId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);
    }

    private Long extrairUsuarioIdDaSessao(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                String[] params = uri.getQuery().split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {

                        String tokenEncoded = param.substring(6);
                        String token = URLDecoder.decode(tokenEncoded, StandardCharsets.UTF_8);
                        var claims = jwtUtil.parseClaims(token);
                        Long usuarioId = Long.parseLong(claims.getSubject());
                        System.out.println("[WEBSOCKET] Token extraído da URL com sucesso para usuário ID: " + usuarioId);
                        return usuarioId;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WEBSOCKET] Falha ao extrair token da URL: " + e.getMessage());
        }
        return null;
    }

    public void enviarParaUsuario(Long usuarioId, Object payload) {
        if (usuarioId == null) return;
        Set<WebSocketSession> sessions = userSessions.get(usuarioId);
        if (sessions == null || sessions.isEmpty()) {
            System.out.println("[WEBSOCKET] Nenhuma sessão ativa para o usuário " + usuarioId + " no momento.");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        System.err.println("[WEBSOCKET] Falha ao enviar para sessão " + session.getId() + ": " + e.getMessage());
                    }
                }
            }
            System.out.println("[WEBSOCKET] Notificação enviada ao usuário ID " + usuarioId + ": " + json);
        } catch (Exception e) {
            System.err.println("[WEBSOCKET] Erro ao serializar notificação: " + e.getMessage());
        }
    }
}

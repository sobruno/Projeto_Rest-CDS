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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de conexões WebSocket para notificações em tempo real.
 * Permite notificar usuários individualmente (ex: saldo atualizado) ou broadcast.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mapeamento: usuarioId -> conjunto de sessões ativas (ex: várias abas abertas)
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // Mapeamento: sessionId -> usuarioId
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
            // Permite autenticação via primeira mensagem enviada pelo cliente: {"tipo":"AUTH", "token":"..."}
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
                        String token = param.substring(6);
                        var claims = jwtUtil.parseClaims(token);
                        return Long.parseLong(claims.getSubject());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Envia mensagem JSON para todas as abas/dispositivos do usuário informado.
     */
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

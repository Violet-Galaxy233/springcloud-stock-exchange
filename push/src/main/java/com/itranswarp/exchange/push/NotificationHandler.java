package com.itranswarp.exchange.push;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 处理器。维护 userId -> 会话集合 的映射以及全部会话集合，
 * 支持向指定用户推送和向所有连接广播。
 */
@Component
public class NotificationHandler extends TextWebSocketHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // userId -> 该用户的所有会话
    private final Map<Long, Set<WebSocketSession>> userToSessions = new ConcurrentHashMap<>();

    // 所有已连接的会话
    private final Set<WebSocketSession> allSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        allSessions.add(session);
        Long userId = parseUserId(session);
        if (userId != null) {
            session.getAttributes().put("userId", userId);
            userToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        }
        logger.info("websocket connected: session={}, userId={}", session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        allSessions.remove(session);
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long) {
            Set<WebSocketSession> sessions = userToSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userToSessions.remove((Long) userId);
                }
            }
        }
        logger.info("websocket closed: session={}, status={}", session.getId(), status);
    }

    /**
     * 向指定用户的所有会话推送消息。
     */
    public void sendToUser(Long userId, String message) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = userToSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            sendMessage(session, textMessage);
        }
    }

    /**
     * 向所有连接广播消息。
     */
    public void broadcast(String message) {
        if (allSessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : allSessions) {
            sendMessage(session, textMessage);
        }
    }

    /**
     * 从握手 URI 的查询参数 token 中解析用户 ID (?token=123)。
     */
    private Long parseUserId(WebSocketSession session) {
        try {
            if (session.getUri() == null) {
                return null;
            }
            String query = session.getUri().getQuery();
            if (query == null) {
                return null;
            }
            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                    String value = pair.substring(idx + 1);
                    if (!value.isEmpty()) {
                        return Long.valueOf(value);
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("cannot parse userId from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 安全发送：跳过已关闭会话，捕获 IO 异常。WebSocketSession 并发发送不安全，故加锁。
     */
    private void sendMessage(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            logger.warn("failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }
}

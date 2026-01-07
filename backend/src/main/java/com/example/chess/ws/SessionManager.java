package com.example.chess.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SessionManager {
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public void unregister(Long userId) {
        sessions.remove(userId);
    }

    public WebSocketSession get(Long userId) {
        return sessions.get(userId);
    }

    public Map<Long, WebSocketSession> all() {
        return sessions;
    }
}

package com.example.chess.ws;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OnlineUserRegistry {
    private final Map<Long, OnlineUser> onlineUsers = new ConcurrentHashMap<>();

    public void setOnline(Long userId, String username) {
        onlineUsers.put(userId, new OnlineUser(userId, username));
    }

    public void setOffline(Long userId) {
        onlineUsers.remove(userId);
    }

    public Collection<OnlineUser> listOnline() {
        return onlineUsers.values();
    }
}

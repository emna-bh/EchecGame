package com.example.chess.config;

import com.example.chess.ws.ChessWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChessWebSocketHandler chessWebSocketHandler;

    public WebSocketConfig(ChessWebSocketHandler chessWebSocketHandler) {
        this.chessWebSocketHandler = chessWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chessWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}

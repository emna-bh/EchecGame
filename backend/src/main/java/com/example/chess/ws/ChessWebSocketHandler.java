package com.example.chess.ws;

import com.example.chess.auth.AuthService;
import com.example.chess.auth.UserEntity;
import com.example.chess.game.GameEntity;
import com.example.chess.game.GameService;
import com.example.chess.game.MoveEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {
    private static final String USER_ID_KEY = "userId";
    private static final String USERNAME_KEY = "username";

    private final AuthService authService;
    private final GameService gameService;
    private final SessionManager sessionManager;
    private final OnlineUserRegistry onlineUserRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public ChessWebSocketHandler(AuthService authService,
                                 GameService gameService,
                                 SessionManager sessionManager,
                                 OnlineUserRegistry onlineUserRegistry) {
        this.authService = authService;
        this.gameService = gameService;
        this.sessionManager = sessionManager;
        this.onlineUserRegistry = onlineUserRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri());
        UserEntity user = token == null ? null : authService.requireUser(token);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        session.getAttributes().put(USER_ID_KEY, user.getId());
        session.getAttributes().put(USERNAME_KEY, user.getUsername());
        sessionManager.register(user.getId(), session);
        onlineUserRegistry.setOnline(user.getId(), user.getUsername());
        broadcastOnlineUsers();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode payload = objectMapper.readTree(message.getPayload());
        String type = payload.path("type").asText(null);
        if (type == null) {
            sendError(session, "Missing message type");
            return;
        }
        switch (type) {
            case "invite" -> handleInvite(session, payload);
            case "invite_response" -> handleInviteResponse(session, payload);
            case "move" -> handleMove(session, payload);
            case "resign" -> handleResign(session, payload);
            default -> sendError(session, "Unknown message type");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(USER_ID_KEY);
        if (userId != null) {
            sessionManager.unregister(userId);
            onlineUserRegistry.setOffline(userId);
            broadcastOnlineUsers();
        }
    }

    private void handleInvite(WebSocketSession session, JsonNode payload) throws IOException {
        Long toUserId = parseLong(payload.get("toUserId"));
        if (toUserId == null) {
            sendError(session, "Invalid target user");
            return;
        }
        WebSocketSession target = sessionManager.get(toUserId);
        if (target == null || !target.isOpen()) {
            sendError(session, "User is offline");
            return;
        }
        Map<String, Object> invite = new HashMap<>();
        invite.put("type", "invite");
        invite.put("fromUserId", session.getAttributes().get(USER_ID_KEY));
        invite.put("fromUsername", session.getAttributes().get(USERNAME_KEY));
        sendMessage(target, invite);

        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "invite_sent");
        ack.put("toUserId", toUserId);
        ack.put("toUsername", target.getAttributes().get(USERNAME_KEY));
        sendMessage(session, ack);
    }

    private void handleInviteResponse(WebSocketSession session, JsonNode payload) throws IOException {
        Long fromUserId = parseLong(payload.get("fromUserId"));
        boolean accepted = payload.path("accepted").asBoolean(false);
        if (fromUserId == null) {
            sendError(session, "Invalid response payload");
            return;
        }
        WebSocketSession inviter = sessionManager.get(fromUserId);
        if (inviter == null || !inviter.isOpen()) {
            sendError(session, "Inviter is offline");
            return;
        }
        if (!accepted) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "invite_response");
            response.put("accepted", false);
            response.put("fromUserId", session.getAttributes().get(USER_ID_KEY));
            sendMessage(inviter, response);
            return;
        }
        Long responderId = (Long) session.getAttributes().get(USER_ID_KEY);
        Long whiteId = random.nextBoolean() ? responderId : fromUserId;
        Long blackId = whiteId.equals(responderId) ? fromUserId : responderId;
        GameEntity game = gameService.createGame(whiteId, blackId);
        sendGameStart(inviter, game, fromUserId, responderId);
        sendGameStart(session, game, responderId, fromUserId);
    }

    private void handleMove(WebSocketSession session, JsonNode payload) throws IOException {
        Long gameId = parseLong(payload.get("gameId"));
        String from = payload.path("from").asText(null);
        String to = payload.path("to").asText(null);
        if (gameId == null || from == null || to == null) {
            sendError(session, "Invalid move payload");
            return;
        }
        GameEntity game = gameService.getGame(gameId);
        if (game == null) {
            sendError(session, "Game not found");
            return;
        }
        if (game.getStatus() == com.example.chess.game.GameStatus.FINISHED) {
            sendError(session, "Game already finished");
            return;
        }
        Long userId = (Long) session.getAttributes().get(USER_ID_KEY);
        if (!userId.equals(game.getWhiteUserId()) && !userId.equals(game.getBlackUserId())) {
            sendError(session, "Not a player in this game");
            return;
        }
        long count = gameService.countMoves(gameId);
        boolean whiteTurn = count % 2 == 0;
        if (whiteTurn && !userId.equals(game.getWhiteUserId())) {
            sendError(session, "Not your turn");
            return;
        }
        if (!whiteTurn && !userId.equals(game.getBlackUserId())) {
            sendError(session, "Not your turn");
            return;
        }
        MoveEntity move;
        try {
            move = gameService.validateAndAddMove(game, from, to, userId);
        } catch (IllegalArgumentException ex) {
            sendError(session, ex.getMessage());
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("type", "move");
        event.put("gameId", gameId);
        event.put("from", move.getFromSquare());
        event.put("to", move.getToSquare());
        event.put("piece", move.getPiece());
        event.put("moveNumber", move.getMoveNumber());
        event.put("byUserId", move.getByUserId());
        broadcastToGame(game, event);
    }

    private void handleResign(WebSocketSession session, JsonNode payload) throws IOException {
        Long gameId = parseLong(payload.get("gameId"));
        if (gameId == null) {
            sendError(session, "Invalid resign payload");
            return;
        }
        GameEntity game = gameService.getGame(gameId);
        if (game == null) {
            sendError(session, "Game not found");
            return;
        }
        if (game.getStatus() == com.example.chess.game.GameStatus.FINISHED) {
            sendError(session, "Game already finished");
            return;
        }
        Long userId = (Long) session.getAttributes().get(USER_ID_KEY);
        if (!userId.equals(game.getWhiteUserId()) && !userId.equals(game.getBlackUserId())) {
            sendError(session, "Not a player in this game");
            return;
        }
        Long winnerId = userId.equals(game.getWhiteUserId()) ? game.getBlackUserId() : game.getWhiteUserId();
        GameEntity finished = gameService.finishGame(game, winnerId, "resign");
        Map<String, Object> event = new HashMap<>();
        event.put("type", "game_over");
        event.put("gameId", finished.getId());
        event.put("winnerUserId", finished.getWinnerUserId());
        event.put("endReason", finished.getEndReason());
        broadcastToGame(finished, event);
    }

    private void sendGameStart(WebSocketSession session, GameEntity game, Long selfId, Long opponentId) throws IOException {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "game_start");
        event.put("gameId", game.getId());
        event.put("color", selfId.equals(game.getWhiteUserId()) ? "white" : "black");
        event.put("opponentId", opponentId);
        event.put("moves", List.of());
        sendMessage(session, event);
    }

    private void broadcastToGame(GameEntity game, Map<String, Object> event) throws IOException {
        WebSocketSession white = sessionManager.get(game.getWhiteUserId());
        WebSocketSession black = sessionManager.get(game.getBlackUserId());
        if (white != null && white.isOpen()) {
            sendMessage(white, event);
        }
        if (black != null && black.isOpen()) {
            sendMessage(black, event);
        }
    }

    private void broadcastOnlineUsers() {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "online_users");
        message.put("users", onlineUserRegistry.listOnline());
        sessionManager.all().values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (IOException ignored) {
                }
            }
        });
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("message", message);
        sendMessage(session, error);
    }

    private void sendMessage(WebSocketSession session, Object payload) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        session.sendMessage(new TextMessage(json));
    }

    private String extractToken(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String[] parts = uri.getQuery().split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("token")) {
                return keyValue[1];
            }
        }
        return null;
    }

    private Long parseLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

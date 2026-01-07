package com.example.chess.game;

import java.util.List;

public record GameStateDto(Long gameId,
                           Long whiteUserId,
                           Long blackUserId,
                           GameStatus status,
                           Long winnerUserId,
                           String endReason,
                           List<MoveDto> moves) {
}

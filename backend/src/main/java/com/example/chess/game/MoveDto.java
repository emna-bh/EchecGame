package com.example.chess.game;

import java.time.Instant;

public record MoveDto(Long id, int moveNumber, String fromSquare, String toSquare, String piece, Long byUserId, Instant createdAt) {
    public static MoveDto fromEntity(MoveEntity entity) {
        return new MoveDto(
                entity.getId(),
                entity.getMoveNumber(),
                entity.getFromSquare(),
                entity.getToSquare(),
                entity.getPiece(),
                entity.getByUserId(),
                entity.getCreatedAt());
    }
}

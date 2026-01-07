package com.example.chess.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "moves")
public class MoveEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private int moveNumber;

    @Column(nullable = false, length = 2)
    private String fromSquare;

    @Column(nullable = false, length = 2)
    private String toSquare;

    @Column(nullable = false, length = 2)
    private String piece;

    @Column(nullable = false)
    private Long byUserId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected MoveEntity() {
    }

    public MoveEntity(Long gameId, int moveNumber, String fromSquare, String toSquare, String piece, Long byUserId) {
        this.gameId = gameId;
        this.moveNumber = moveNumber;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.piece = piece;
        this.byUserId = byUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getGameId() {
        return gameId;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public String getFromSquare() {
        return fromSquare;
    }

    public String getToSquare() {
        return toSquare;
    }

    public String getPiece() {
        return piece;
    }

    public Long getByUserId() {
        return byUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

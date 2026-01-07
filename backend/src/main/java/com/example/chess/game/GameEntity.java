package com.example.chess.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "games")
public class GameEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long whiteUserId;

    @Column(nullable = false)
    private Long blackUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.ACTIVE;

    @Column
    private Long winnerUserId;

    @Column(length = 30)
    private String endReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Column
    private Instant endedAt;

    protected GameEntity() {
    }

    public GameEntity(Long whiteUserId, Long blackUserId) {
        this.whiteUserId = whiteUserId;
        this.blackUserId = blackUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getWhiteUserId() {
        return whiteUserId;
    }

    public Long getBlackUserId() {
        return blackUserId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Long getWinnerUserId() {
        return winnerUserId;
    }

    public String getEndReason() {
        return endReason;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void finish(Long winnerUserId, String endReason) {
        this.status = GameStatus.FINISHED;
        this.winnerUserId = winnerUserId;
        this.endReason = endReason;
        this.endedAt = Instant.now();
        this.updatedAt = this.endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

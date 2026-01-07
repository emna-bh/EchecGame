package com.example.chess.game;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<GameEntity, Long> {
    @Query("select g from GameEntity g where g.status = 'ACTIVE' and (g.whiteUserId = :userId or g.blackUserId = :userId)")
    Optional<GameEntity> findActiveByUserId(@Param("userId") Long userId);
}

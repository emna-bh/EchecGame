package com.example.chess.game;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoveRepository extends JpaRepository<MoveEntity, Long> {
    List<MoveEntity> findByGameIdOrderByMoveNumber(Long gameId);

    long countByGameId(Long gameId);
}

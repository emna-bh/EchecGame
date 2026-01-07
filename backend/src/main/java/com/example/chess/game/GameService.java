package com.example.chess.game;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;

    public GameService(GameRepository gameRepository, MoveRepository moveRepository) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
    }

    public GameEntity createGame(Long whiteUserId, Long blackUserId) {
        GameEntity game = new GameEntity(whiteUserId, blackUserId);
        return gameRepository.save(game);
    }

    public GameEntity getGame(Long gameId) {
        return gameRepository.findById(gameId).orElse(null);
    }

    public List<MoveDto> getMoves(Long gameId) {
        return moveRepository.findByGameIdOrderByMoveNumber(gameId)
                .stream()
                .map(MoveDto::fromEntity)
                .toList();
    }

    public GameStateDto getActiveGame(Long userId) {
        return gameRepository.findActiveByUserId(userId)
                .map(game -> new GameStateDto(
                        game.getId(),
                        game.getWhiteUserId(),
                        game.getBlackUserId(),
                        game.getStatus(),
                        game.getWinnerUserId(),
                        game.getEndReason(),
                        getMoves(game.getId())))
                .orElse(null);
    }

    public MoveEntity addMove(Long gameId, String fromSquare, String toSquare, String piece, Long byUserId) {
        long count = moveRepository.countByGameId(gameId);
        MoveEntity move = new MoveEntity(gameId, (int) count + 1, fromSquare, toSquare, piece, byUserId);
        return moveRepository.save(move);
    }

    public long countMoves(Long gameId) {
        return moveRepository.countByGameId(gameId);
    }

    public MoveEntity validateAndAddMove(GameEntity game, String fromSquare, String toSquare, Long byUserId) {
        List<MoveEntity> moves = moveRepository.findByGameIdOrderByMoveNumber(game.getId());
        ChessRules.BoardState state = ChessRules.buildBoard(moves);
        ChessRules.Square from = ChessRules.Square.parse(fromSquare);
        ChessRules.Square to = ChessRules.Square.parse(toSquare);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Invalid square notation");
        }
        String piece = state.getPiece(from);
        if (piece == null || piece.isEmpty()) {
            throw new IllegalArgumentException("No piece on source square");
        }
        if (piece.startsWith("w") && !byUserId.equals(game.getWhiteUserId())) {
            throw new IllegalArgumentException("Not your piece");
        }
        if (piece.startsWith("b") && !byUserId.equals(game.getBlackUserId())) {
            throw new IllegalArgumentException("Not your piece");
        }
        if (!ChessRules.isLegalMove(state, fromSquare, toSquare)) {
            throw new IllegalArgumentException("Illegal move");
        }
        MoveEntity move = new MoveEntity(game.getId(), moves.size() + 1, fromSquare, toSquare, piece, byUserId);
        return moveRepository.save(move);
    }

    public GameEntity finishGame(GameEntity game, Long winnerUserId, String endReason) {
        game.finish(winnerUserId, endReason);
        return gameRepository.save(game);
    }
}

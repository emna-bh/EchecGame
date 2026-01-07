package com.example.chess.game;

import com.example.chess.auth.AuthService;
import com.example.chess.auth.UserEntity;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final AuthService authService;
    private final GameService gameService;

    public GameController(AuthService authService, GameService gameService) {
        this.authService = authService;
        this.gameService = gameService;
    }

    @GetMapping("/active")
    public ResponseEntity<GameStateDto> activeGame(@RequestHeader("Authorization") String authorization) {
        UserEntity user = requireUser(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GameStateDto state = gameService.getActiveGame(user.getId());
        return ResponseEntity.ok(state);
    }

    @GetMapping("/{gameId}/moves")
    public ResponseEntity<List<MoveDto>> moves(@RequestHeader("Authorization") String authorization,
                                               @PathVariable Long gameId) {
        UserEntity user = requireUser(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GameEntity game = gameService.getGame(gameId);
        if (game == null || (!game.getWhiteUserId().equals(user.getId()) && !game.getBlackUserId().equals(user.getId()))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(gameService.getMoves(gameId));
    }

    private UserEntity requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return authService.requireUser(token);
    }
}

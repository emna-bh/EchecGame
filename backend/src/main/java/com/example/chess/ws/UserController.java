package com.example.chess.ws;

import com.example.chess.auth.AuthService;
import com.example.chess.auth.UserEntity;
import java.util.Collection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;
    private final OnlineUserRegistry onlineUserRegistry;

    public UserController(AuthService authService, OnlineUserRegistry onlineUserRegistry) {
        this.authService = authService;
        this.onlineUserRegistry = onlineUserRegistry;
    }

    @GetMapping("/online")
    public ResponseEntity<Collection<OnlineUser>> online(@RequestHeader("Authorization") String authorization) {
        UserEntity user = requireUser(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(onlineUserRegistry.listOnline());
    }

    private UserEntity requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return authService.requireUser(token);
    }
}

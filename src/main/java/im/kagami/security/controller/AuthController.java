package im.kagami.security.controller;

import im.kagami.security.annotation.AnonymousAccess;
import im.kagami.security.dto.LoginRequest;
import im.kagami.security.dto.LoginResponse;
import im.kagami.security.dto.RegisterRequest;
import im.kagami.security.ratelimit.annotation.RateLimit;
import im.kagami.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Anonymous reachability is declared per endpoint with
 * {@link AnonymousAccess}; everything else requires a valid JWT.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @RateLimit(capacity = 5, duration = 1)
    @AnonymousAccess
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest.username(), loginRequest.password());
    }

    @AnonymousAccess
    @GetMapping("/ciallo")
    public String ciallo() {
        return "Ciallo～(∠・ω< )⌒★";
    }

    @RateLimit(capacity = 3, duration = 1)
    @AnonymousAccess
    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest.username(), registerRequest.password(), registerRequest.nickname());
    }
}

package com.taskboard.controller;

import com.taskboard.model.dto.AuthResponse;
import com.taskboard.model.dto.LoginRequest;
import com.taskboard.model.dto.RefreshTokenRequest;
import com.taskboard.model.dto.RegisterRequest;
import com.taskboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication operations.
 * Handles user login, registration, and token refresh.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint.
     *
     * @param loginRequest the login request
     * @return ResponseEntity with authentication response
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for user: {}", loginRequest.getUsername());
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Register endpoint.
     *
     * @param registerRequest the registration request
     * @return ResponseEntity with authentication response
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration request received for user: {}", registerRequest.getUsername());
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refresh token endpoint.
     *
     * @param request the refresh token request
     * @return ResponseEntity with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        String newAccessToken = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken));
    }

    /**
     * Response DTO for token refresh.
     */
    private record TokenRefreshResponse(String accessToken, String tokenType) {
        TokenRefreshResponse(String accessToken) {
            this(accessToken, "Bearer");
        }
    }
}


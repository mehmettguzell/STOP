package com.stop.identity_service.user.controller;

import com.stop.identity_service.config.jwt.SecurityUtils;
import com.stop.identity_service.user.dto.request.LoginRequest;
import com.stop.identity_service.user.dto.request.RefreshRequest;
import com.stop.identity_service.user.dto.request.RegisterRequest;
import com.stop.identity_service.user.dto.response.AuthResponse;
import com.stop.identity_service.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}

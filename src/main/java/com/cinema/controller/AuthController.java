package com.cinema.controller;

import com.cinema.dto.request.LoginRequest;
import com.cinema.dto.request.RegisterRequest;
import com.cinema.dto.response.UserResponse;
import com.cinema.service.AuthService; // Предположим, у вас есть такой сервис
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Возвращает JWT токен
        return ResponseEntity.ok(authService.login(request));
    }
}
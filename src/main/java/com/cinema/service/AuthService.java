package com.cinema.service;

import com.cinema.dto.request.LoginRequest;
import com.cinema.dto.request.RegisterRequest;
import com.cinema.dto.response.UserResponse;
import com.cinema.entity.User;
import com.cinema.entity.UserRole;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.UserRepository;
import com.cinema.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessLogicException("Пользователь с таким email уже существует");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.USER) // По умолчанию роль USER
                .build();

        userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessLogicException("Неверный email или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessLogicException("Неверный email или пароль");
        }

        // Генерируем токен с ролью пользователя
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }
}
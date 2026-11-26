package com.cinema.controller;

import com.cinema.dto.request.HallRequest;
import com.cinema.dto.response.GenreResponse;
import com.cinema.dto.response.HallResponse;
import com.cinema.dto.response.UserResponse;
import com.cinema.entity.Genre;
import com.cinema.entity.Hall;
import com.cinema.entity.Seat;
import com.cinema.entity.User;
import com.cinema.entity.UserRole;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.HallRepository;
import com.cinema.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final HallRepository hallRepository;

    // --- Управление пользователями ---

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .role(u.getRole())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    @Transactional
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long id,
            @RequestParam UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessLogicException("Пользователь не найден"));
        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.ok(UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessLogicException("Пользователь не найден");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Справочник жанров ---

    @PostMapping("/genres")
    public ResponseEntity<GenreResponse> createGenre(@RequestParam String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessLogicException("Название жанра обязательно");
        }
        Genre genre = new Genre(name.trim());
        Genre saved = genreRepository.save(genre);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenreResponse(saved.getId(), saved.getName()));
    }

    @DeleteMapping("/genres/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        if (!genreRepository.existsById(id)) {
            throw new BusinessLogicException("Жанр не найден");
        }
        genreRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Справочник залов и генерация мест ---

    @GetMapping("/halls")
    public ResponseEntity<List<HallResponse>> getAllHalls() {
        List<HallResponse> halls = hallRepository.findAll().stream()
                .map(this::mapHallToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(halls);
    }

    @PostMapping("/halls")
    @Transactional
    public ResponseEntity<HallResponse> createHall(@Valid @RequestBody HallRequest request) {
        if (hallRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessLogicException("Зал с таким названием уже существует");
        }

        int capacity = request.getRowsCount() * request.getSeatsPerRow();
        Hall hall = new Hall(request.getName(), capacity);
        hall.setDescription(request.getDescription());
        hall.setIsActive(true);

        List<Seat> seats = new ArrayList<>(capacity);
        Set<Integer> vipRows = request.getVipRows() != null ? request.getVipRows() : Set.of();

        for (int row = 1; row <= request.getRowsCount(); row++) {
            String seatType = vipRows.contains(row) ? "VIP" : "STANDARD";
            for (int seatNum = 1; seatNum <= request.getSeatsPerRow(); seatNum++) {
                Seat seat = new Seat(hall, row, seatNum);
                seat.setSeatType(seatType);
                seats.add(seat);
            }
        }
        hall.setSeats(seats);

        Hall saved = hallRepository.save(hall);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapHallToResponse(saved));
    }

    @DeleteMapping("/halls/{id}")
    public ResponseEntity<Void> deleteHall(@PathVariable Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new BusinessLogicException("Зал не найден"));
        hall.setIsActive(false);
        hallRepository.save(hall);
        return ResponseEntity.noContent().build();
    }

    private HallResponse mapHallToResponse(Hall hall) {
        return HallResponse.builder()
                .id(hall.getId())
                .name(hall.getName())
                .capacity(hall.getCapacity())
                .description(hall.getDescription())
                .isActive(hall.getIsActive())
                .build();
    }
}

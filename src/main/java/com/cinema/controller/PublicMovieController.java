package com.cinema.controller;

import com.cinema.dto.response.MovieResponse;
import com.cinema.dto.response.SessionResponse;
import com.cinema.service.MovieService;
import com.cinema.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public/movies")
@RequiredArgsConstructor
public class PublicMovieController {

    private final MovieService movieService;
    private final SessionService sessionService;

    // Получить все активные фильмы
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    // Поиск фильма по названию
    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(movieService.searchMovies(query));
    }

    // Расписание сеансов фильма на конкретную дату
    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.getSessionsByMovieAndDate(id, date));
    }
}
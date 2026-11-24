package com.cinema.controller;

import com.cinema.dto.response.SessionSeatsResponse;
import com.cinema.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/sessions")
@RequiredArgsConstructor
public class PublicSessionController {

    private final SessionService sessionService;

    /**
     * Получить интерактивную схему зала и статус доступности всех мест для конкретного сеанса
     */
    @GetMapping("/{id}/seats")
    public ResponseEntity<SessionSeatsResponse> getSessionSeats(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionSeats(id));
    }
}

package com.cinema.service;

import com.cinema.dto.request.SessionRequest;
import com.cinema.dto.response.SessionResponse;
import com.cinema.dto.response.SessionSeatsResponse;

import java.time.LocalDate;
import java.util.List;

public interface SessionService {
    SessionResponse createSession(SessionRequest request);
    SessionResponse updateSession(Long id, SessionRequest request);
    List<SessionResponse> getSessionsByDate(LocalDate date);
    List<SessionResponse> getSessionsByMovie(Long movieId);
    List<SessionResponse> getSessionsByMovieAndDate(Long movieId, LocalDate date);
    SessionSeatsResponse getSessionSeats(Long sessionId);
    void deleteSession(Long id);
}
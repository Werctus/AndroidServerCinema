package com.cinema.service;

import com.cinema.dto.request.SessionRequest;
import com.cinema.dto.response.SessionResponse;
import java.time.LocalDate;
import java.util.List;

public interface SessionService {
    SessionResponse createSession(SessionRequest request);
    List<SessionResponse> getSessionsByDate(LocalDate date);
    List<SessionResponse> getSessionsByMovie(Long movieId);
    void deleteSession(Long id);
}
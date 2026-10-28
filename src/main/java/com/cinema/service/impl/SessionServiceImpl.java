package com.cinema.service.impl;

import com.cinema.dto.request.SessionRequest;
import com.cinema.dto.response.SessionResponse;
import com.cinema.entity.Hall;
import com.cinema.entity.Movie;
import com.cinema.entity.Session;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.HallRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.SessionRepository;
import com.cinema.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;

    @Override
    @Transactional
    public SessionResponse createSession(SessionRequest request) {
        // 1. Получаем связанные сущности
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new BusinessLogicException("Фильм не найден"));

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new BusinessLogicException("Зал не найден"));

        // 2. БИЗНЕС-ВАЛИДАЦИЯ: Проверка пересечения времени
        // Рассчитываем время окончания нового сеанса
        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());

        List<Session> overlapping = sessionRepository.findOverlappingSessions(
                hall.getId(),
                request.getStartTime(),
                endTime
        );

        if (!overlapping.isEmpty()) {
            throw new BusinessLogicException(
                    "В выбранном зале уже есть сеанс в это время. " +
                            "Конфликт с фильмом: " + overlapping.get(0).getMovie().getTitle()
            );
        }

        // 3. Создание и сохранение
        Session session = new Session(movie, hall, request.getStartTime(), request.getPrice());
        session.setFormat(request.getFormat() != null ? request.getFormat() : "2D");

        return mapToResponse(sessionRepository.save(session));
    }

    @Override
    public List<SessionResponse> getSessionsByDate(LocalDate date) {
        return sessionRepository.findByStartTimeDate(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SessionResponse> getSessionsByMovie(Long movieId) {
        return sessionRepository.findByMovieId(movieId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSession(Long id) {
        // В реальном проекте нельзя удалять сеанс, если на него есть брони!
        // Здесь для простоты просто удаляем, но лучше добавить проверку
        sessionRepository.deleteById(id);
    }

    private SessionResponse mapToResponse(Session session) {
        return SessionResponse.builder()
                .id(session.getId())
                .movieTitle(session.getMovie().getTitle())
                .hallName(session.getHall().getName())
                .startTime(session.getStartTime())
                .price(session.getPrice())
                .format(session.getFormat())
                .duration(session.getMovie().getDuration())
                .build();
    }
}
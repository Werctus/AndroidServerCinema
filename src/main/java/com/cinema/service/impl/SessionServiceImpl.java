package com.cinema.service.impl;

import com.cinema.dto.request.SessionRequest;
import com.cinema.dto.response.SessionResponse;
import com.cinema.dto.response.SessionSeatsResponse;
import com.cinema.entity.BookingStatus;
import com.cinema.entity.Hall;
import com.cinema.entity.Movie;
import com.cinema.entity.Seat;
import com.cinema.entity.Session;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.BookedSeatRepository;
import com.cinema.repository.BookingRepository;
import com.cinema.repository.HallRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.SeatRepository;
import com.cinema.repository.SessionRepository;
import com.cinema.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public SessionResponse createSession(SessionRequest request) {
        // 1. Получаем связанные сущности
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new BusinessLogicException("Фильм не найден"));

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new BusinessLogicException("Зал не найден"));

        validateRentalPeriod(movie, request.getStartTime().toLocalDate());

        // 2. БИЗНЕС-ВАЛИДАЦИЯ: Проверка пересечения времени
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
    @Transactional
    public SessionResponse updateSession(Long id, SessionRequest request) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new BusinessLogicException("Сеанс не найден"));

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new BusinessLogicException("Фильм не найден"));

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new BusinessLogicException("Зал не найден"));

        validateRentalPeriod(movie, request.getStartTime().toLocalDate());

        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());
        List<Session> overlapping = sessionRepository.findOverlappingSessions(
                        hall.getId(),
                        request.getStartTime(),
                        endTime
                ).stream()
                .filter(s -> !s.getId().equals(id))
                .collect(Collectors.toList());

        if (!overlapping.isEmpty()) {
            throw new BusinessLogicException(
                    "В выбранном зале уже есть сеанс в это время. " +
                            "Конфликт с фильмом: " + overlapping.get(0).getMovie().getTitle()
            );
        }

        session.setMovie(movie);
        session.setHall(hall);
        session.setStartTime(request.getStartTime());
        session.setPrice(request.getPrice());
        if (request.getFormat() != null) {
            session.setFormat(request.getFormat());
        }

        return mapToResponse(sessionRepository.save(session));
    }

    private void validateRentalPeriod(Movie movie, LocalDate sessionDate) {
        if (movie.getReleaseDate() != null && sessionDate.isBefore(movie.getReleaseDate())) {
            throw new BusinessLogicException("Сеанс не может быть назначен раньше даты выхода фильма в прокат: " + movie.getReleaseDate());
        }
        if (movie.getEndDate() != null && sessionDate.isAfter(movie.getEndDate())) {
            throw new BusinessLogicException("Сеанс не может быть назначен после окончания проката фильма: " + movie.getEndDate());
        }
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
    public List<SessionResponse> getSessionsByMovieAndDate(Long movieId, LocalDate date) {
        return sessionRepository.findByMovieIdAndDate(movieId, date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SessionSeatsResponse getSessionSeats(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessLogicException("Сеанс не найден"));

        List<Seat> seats = seatRepository.findByHallIdOrderByRowNumberAscSeatNumberAsc(session.getHall().getId());

        Set<Long> occupiedSeatIds = bookedSeatRepository.findOccupiedSeatsBySessionId(sessionId)
                .stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        List<SessionSeatsResponse.SeatDto> seatDtos = seats.stream()
                .map(seat -> {
                    boolean isAvailable = !occupiedSeatIds.contains(seat.getId());
                    BigDecimal price = "VIP".equalsIgnoreCase(seat.getSeatType())
                            ? session.getPrice().multiply(new BigDecimal("1.5"))
                            : session.getPrice();

                    return SessionSeatsResponse.SeatDto.builder()
                            .id(seat.getId())
                            .rowNumber(seat.getRowNumber())
                            .seatNumber(seat.getSeatNumber())
                            .seatType(seat.getSeatType() != null ? seat.getSeatType() : "STANDARD")
                            .price(price)
                            .isAvailable(isAvailable)
                            .build();
                })
                .collect(Collectors.toList());

        int totalRows = seats.stream().mapToInt(Seat::getRowNumber).max().orElse(0);

        return SessionSeatsResponse.builder()
                .sessionId(session.getId())
                .movieId(session.getMovie().getId())
                .movieTitle(session.getMovie().getTitle())
                .hallId(session.getHall().getId())
                .hallName(session.getHall().getName())
                .startTime(session.getStartTime())
                .basePrice(session.getPrice())
                .format(session.getFormat())
                .totalRows(totalRows)
                .totalSeats(seats.size())
                .seats(seatDtos)
                .build();
    }

    @Override
    @Transactional
    public void deleteSession(Long id) {
        if (!sessionRepository.existsById(id)) {
            throw new BusinessLogicException("Сеанс не найден");
        }
        boolean hasActiveBookings = !bookingRepository
                .findBySessionIdAndStatusIn(id, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .isEmpty();
        if (hasActiveBookings) {
            throw new BusinessLogicException("Нельзя удалить сеанс, на который есть активные бронирования");
        }
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
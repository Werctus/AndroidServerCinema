package com.cinema.service;

import com.cinema.dto.request.SessionRequest;
import com.cinema.dto.response.SessionSeatsResponse;
import com.cinema.entity.BookedSeat;
import com.cinema.entity.Booking;
import com.cinema.entity.Hall;
import com.cinema.entity.Movie;
import com.cinema.entity.Seat;
import com.cinema.entity.Session;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.BookedSeatRepository;
import com.cinema.repository.HallRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.SeatRepository;
import com.cinema.repository.SessionRepository;
import com.cinema.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private HallRepository hallRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private BookedSeatRepository bookedSeatRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private Movie movie;
    private Hall hall;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Film");
        movie.setDuration(90);

        hall = new Hall();
        hall.setId(1L);
        hall.setName("Hall 1");
    }

    @Test
    @DisplayName("Создание сеанса при наличии пересечения времени")
    void shouldThrowExceptionWhenSessionOverlaps() {
        // Arrange
        SessionRequest request = new SessionRequest();
        request.setMovieId(1L);
        request.setHallId(1L);
        request.setStartTime(LocalDateTime.of(2026, 11, 5, 18, 0));
        request.setPrice(new BigDecimal("300"));

        // Существующий сеанс: 17:30 - 19:00 (90 мин)
        Session existingSession = new Session(movie, hall, LocalDateTime.of(2026, 11, 5, 17, 30), new BigDecimal("300"));

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        // Новый сеанс начинается в 18:00 и заканчивается в 19:30.
        // Он пересекается с существующим (17:30-19:00)
        when(sessionRepository.findOverlappingSessions(eq(1L), any(), any()))
                .thenReturn(List.of(existingSession));

        // Act & Assert
        assertThatThrownBy(() -> sessionService.createSession(request))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("уже есть сеанс");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение схемы зала и доступности мест для сеанса")
    void shouldReturnSessionSeatsWithAvailability() {
        // Arrange
        Session session = new Session(movie, hall, LocalDateTime.of(2026, 11, 25, 19, 0), new BigDecimal("350"));
        session.setId(10L);

        Seat seat1 = new Seat(hall, 1, 1);
        seat1.setId(101L);
        seat1.setSeatType("STANDARD");

        Seat seat2 = new Seat(hall, 1, 2);
        seat2.setId(102L);
        seat2.setSeatType("VIP");

        Booking booking = new Booking();
        booking.setId(5L);

        BookedSeat bookedSeat = new BookedSeat(booking, seat1, new BigDecimal("350"));

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(seatRepository.findByHallIdOrderByRowNumberAscSeatNumberAsc(hall.getId())).thenReturn(List.of(seat1, seat2));
        when(bookedSeatRepository.findOccupiedSeatsBySessionId(10L)).thenReturn(List.of(bookedSeat));

        // Act
        SessionSeatsResponse response = sessionService.getSessionSeats(10L);

        // Assert
        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals(10L, response.getSessionId());
        org.junit.jupiter.api.Assertions.assertEquals(2, response.getSeats().size());

        // Место 1 занято
        SessionSeatsResponse.SeatDto seatDto1 = response.getSeats().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(101L, seatDto1.getId());
        org.junit.jupiter.api.Assertions.assertFalse(seatDto1.isAvailable());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("350"), seatDto1.getPrice());

        // Место 2 свободно и VIP (с наценкой 1.5)
        SessionSeatsResponse.SeatDto seatDto2 = response.getSeats().get(1);
        org.junit.jupiter.api.Assertions.assertEquals(102L, seatDto2.getId());
        org.junit.jupiter.api.Assertions.assertTrue(seatDto2.isAvailable());
        org.junit.jupiter.api.Assertions.assertEquals("VIP", seatDto2.getSeatType());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("525.0"), seatDto2.getPrice());
    }
}
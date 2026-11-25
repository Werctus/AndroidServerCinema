package com.cinema.service;

import com.cinema.dto.request.BookingRequest;
import com.cinema.dto.response.BookingResponse;
import com.cinema.entity.*;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.*;
import com.cinema.service.impl.BookingServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Включает поддержку Mockito аннотаций
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BookedSeatRepository bookedSeatRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Movie testMovie;
    private Hall testHall;
    private Session testSession;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        // Создаем тестовые данные
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@test.com");

        testMovie = new Movie();
        testMovie.setId(10L);
        testMovie.setTitle("Тестовый Фильм");
        testMovie.setDuration(120);

        testHall = new Hall();
        testHall.setId(20L);
        testHall.setName("Зал 1");

        testSession = new Session(testMovie, testHall, LocalDateTime.now().plusDays(1), new BigDecimal("300"));
        testSession.setId(100L);

        testSeat = new Seat(testHall, 1, 1);
        testSeat.setId(50L);
    }

    @Test
    @DisplayName("Успешное создание бронирования")
    void shouldCreateBookingSuccessfully() {
        // Arrange (Подготовка)
        BookingRequest request = new BookingRequest();
        request.setSessionId(100L);
        request.setSeatIds(List.of(50L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(testSession));
        when(bookedSeatRepository.findOccupiedSeatsBySessionId(100L)).thenReturn(List.of()); // Места свободны
        when(seatRepository.findById(50L)).thenReturn(Optional.of(testSeat));

        // Эмулируем сохранение: возвращаем тот же объект с ID
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act (Действие)
        BookingResponse response = bookingService.createBooking(1L, request);

        // Assert (Проверка)
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(response.getSeats()).hasSize(1);

        // Проверяем, что метод сохранения был вызван ровно 1 раз
        verify(bookingRepository, times(1)).save(any(Booking.class));
        // Проверяем, что уведомление было отправлено
        verify(notificationService, times(1)).sendBookingConfirmation(eq("user@test.com"), any(Booking.class));
    }

    @Test
    @DisplayName("Ошибка при бронировании занятого места")
    void shouldThrowExceptionWhenSeatIsOccupied() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setSessionId(100L);
        request.setSeatIds(List.of(50L));

        // Эмулируем, что место УЖЕ занято другой активной бронью
        BookedSeat occupiedSeat = new BookedSeat();
        Seat occupied = new Seat(testHall, 1, 1);
        occupied.setId(50L);
        occupiedSeat.setSeat(occupied);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(testSession));
        when(bookedSeatRepository.findOccupiedSeatsBySessionId(100L)).thenReturn(List.of(occupiedSeat));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("уже заняты");

        // Убеждаемся, что save НЕ был вызван
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ошибка при бронировании прошедшего сеанса")
    void shouldThrowExceptionForPastSession() {
        // Arrange
        testSession.setStartTime(LocalDateTime.now().minusHours(1)); // Сеанс в прошлом

        BookingRequest request = new BookingRequest();
        request.setSessionId(100L);
        request.setSeatIds(List.of(50L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(testSession));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("прошедший сеанс");
    }

    @Test
    @DisplayName("Подтверждение оплаты и генерация билета с QR-кодом")
    void shouldConfirmPaymentAndGenerateTicket() {
        // Arrange
        Booking pendingBooking = new Booking(testUser, testSession);
        pendingBooking.setId(200L);
        pendingBooking.setStatus(BookingStatus.PENDING);
        pendingBooking.setTotalAmount(new BigDecimal("300"));
        pendingBooking.setBookedSeats(List.of(new BookedSeat(pendingBooking, testSeat, new BigDecimal("300"))));

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.findByBookingId(200L)).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // Act
        BookingResponse response = bookingService.confirmPayment(200L);

        // Assert
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTicket()).isNotNull();
        assertThat(response.getTicket().getTicketNumber()).startsWith("KINO-");
        assertThat(response.getTicket().getQrCode()).isNotBlank();

        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }
}
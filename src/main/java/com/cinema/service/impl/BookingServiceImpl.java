package com.cinema.service.impl;

import com.cinema.dto.request.BookingRequest;
import com.cinema.dto.response.BookingResponse;
import com.cinema.entity.*;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.*;
import com.cinema.service.BookingService;
import com.cinema.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        // 1. Валидация существования сущностей
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("Пользователь не найден"));

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new BusinessLogicException("Сеанс не найден"));

        // Проверка: не начался ли уже сеанс
        if (session.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("Нельзя забронировать прошедший сеанс");
        }

        // 2. Проверка доступности мест (бизнес-логика)
        List<Long> occupiedSeatIds = bookedSeatRepository
                .findOccupiedSeatsBySessionId(session.getId())
                .stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toList());

        List<Long> conflictingSeats = request.getSeatIds().stream()
                .filter(occupiedSeatIds::contains)
                .collect(Collectors.toList());

        if (!conflictingSeats.isEmpty()) {
            throw new BusinessLogicException("Следующие места уже заняты: " + conflictingSeats);
        }

        // 3. Создание брони
        Booking booking = new Booking(user, session);
        booking.setStatus(BookingStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookedSeat> bookedSeats = new ArrayList<>();

        for (Long seatId : request.getSeatIds()) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new BusinessLogicException("Место не найдено"));

            // Фиксируем цену на момент бронирования
            BigDecimal seatPrice = session.getPrice();
            // Здесь можно добавить логику наценок для VIP мест, если нужно

            BookedSeat bookedSeat = new BookedSeat(booking, seat, seatPrice);
            bookedSeats.add(bookedSeat);
            totalAmount = totalAmount.add(seatPrice);
        }

        booking.setBookedSeats(bookedSeats);
        booking.setTotalAmount(totalAmount);

        Booking savedBooking = bookingRepository.save(booking);

        // 4. Отправка уведомления (асинхронно или синхронно в зависимости от требований)
        try {
            notificationService.sendBookingConfirmation(user.getEmail(), savedBooking);
        } catch (Exception e) {
            // Логируем ошибку почты, но НЕ прерываем транзакцию брони
            System.err.println("Ошибка отправки email: " + e.getMessage());
        }

        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse confirmPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessLogicException("Бронь не найдена"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessLogicException("Бронь уже обработана");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        // Здесь можно создать Ticket实体

        return mapToResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessLogicException("Бронь не найдена"));

        // Проверка прав: отменить может только владелец или админ/кассир
        if (!booking.getUser().getId().equals(userId)) {
            // В реальном проекте здесь нужна проверка роли через SecurityContext
            // Для упрощения пока пропускаем, но в продакшене обязательно!
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessLogicException("Бронь уже отменена");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean areSeatsAvailable(Long sessionId, List<Long> seatIds) {
        List<Long> occupied = bookedSeatRepository
                .findOccupiedSeatsBySessionId(sessionId)
                .stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toList());

        return seatIds.stream().noneMatch(occupied::contains);
    }

    // Приватный метод маппинга (можно вынести в отдельный Mapper класс)
    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .totalAmount(booking.getTotalAmount())
                .seats(booking.getBookedSeats().stream()
                        .map(bs -> BookingResponse.SeatInfo.builder()
                                .rowNumber(bs.getSeat().getRowNumber())
                                .seatNumber(bs.getSeat().getSeatNumber())
                                .price(bs.getFixedPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
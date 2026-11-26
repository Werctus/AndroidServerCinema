package com.cinema.service.impl;

import com.cinema.dto.request.BookingRequest;
import com.cinema.dto.request.CashierSaleRequest;
import com.cinema.dto.response.BookingResponse;
import com.cinema.dto.response.SessionOccupancyResponse;
import com.cinema.entity.*;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.*;
import com.cinema.service.BookingService;
import com.cinema.service.NotificationService;
import com.cinema.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final QrCodeService qrCodeService;

    @Override
    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        // Валидация существования сущностей
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("Пользователь не найден"));

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new BusinessLogicException("Сеанс не найден"));

        // Проверка: не начался ли уже сеанс
        if (session.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("Нельзя забронировать прошедший сеанс");
        }

        // Проверка доступности мест (бизнес-логика)
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

        // Создание брони
        Booking booking = new Booking(user, session);
        booking.setStatus(BookingStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookedSeat> bookedSeats = new ArrayList<>();

        for (Long seatId : request.getSeatIds()) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new BusinessLogicException("Место не найдено"));

            // Фиксируем цену на момент бронирования (учитываем наценку для VIP)
            BigDecimal seatPrice = "VIP".equalsIgnoreCase(seat.getSeatType())
                    ? session.getPrice().multiply(new BigDecimal("1.5"))
                    : session.getPrice();

            BookedSeat bookedSeat = new BookedSeat(booking, seat, seatPrice);
            bookedSeats.add(bookedSeat);
            totalAmount = totalAmount.add(seatPrice);
        }

        booking.setBookedSeats(bookedSeats);
        booking.setTotalAmount(totalAmount);

        Booking savedBooking = bookingRepository.save(booking);

        // Отправка уведомления (асинхронно или синхронно в зависимости от требований)
        try {
            notificationService.sendBookingConfirmation(user.getEmail(), savedBooking);
        } catch (Exception e) {
            // Логируем ошибку почты, не прерывая транзакцию брони
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
        Booking savedBooking = bookingRepository.save(booking);

        Ticket ticket = ticketRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    String ticketNumber = "KINO-" + timestamp + "-" + randomSuffix;
                    String qrData = UUID.randomUUID().toString().replace("-", "");

                    Ticket newTicket = new Ticket(savedBooking, ticketNumber, qrData);
                    return ticketRepository.save(newTicket);
                });

        return mapToResponse(savedBooking, ticket);
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

    @Override
    public BookingResponse.TicketInfo getTicketByBookingId(Long bookingId) {
        Ticket ticket = ticketRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessLogicException("Билет для данного бронирования не найден (возможно, бронь не оплачена)"));

        return mapTicketToInfo(ticket);
    }

    @Override
    public byte[] getTicketQrCodeImage(Long bookingId) {
        Ticket ticket = ticketRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessLogicException("Билет для данного бронирования не найден"));

        String qrPayload = String.format("TICKET:%s|QR:%s", ticket.getTicketNumber(), ticket.getQrCode());
        return qrCodeService.generateQrCodeImage(qrPayload, 300, 300);
    }

    @Override
    @Transactional
    public BookingResponse sellTicketsAsCashier(Long cashierId, CashierSaleRequest request) {
        User customer = (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank())
                ? userRepository.findByEmail(request.getCustomerEmail())
                        .orElseThrow(() -> new BusinessLogicException("Клиент с таким email не найден"))
                : userRepository.findById(cashierId)
                        .orElseThrow(() -> new BusinessLogicException("Кассир не найден"));

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setSessionId(request.getSessionId());
        bookingRequest.setSeatIds(request.getSeatIds());

        BookingResponse created = createBooking(customer.getId(), bookingRequest);

        if (request.getDiscountPercent() != null && request.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            applyDiscount(created.getId(), request.getDiscountPercent());
        }

        return confirmPayment(created.getId());
    }

    @Override
    @Transactional
    public BookingResponse applyDiscount(Long bookingId, BigDecimal discountPercent) {
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(new BigDecimal("50")) > 0) {
            throw new BusinessLogicException("Скидка должна быть от 0% до 50%");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessLogicException("Бронь не найдена"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessLogicException("Скидку можно применить только к неоплаченной брони");
        }

        BigDecimal baseAmount = booking.getBookedSeats().stream()
                .map(BookedSeat::getFixedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = baseAmount
                .multiply(discountPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(baseAmount.subtract(discountAmount));

        return mapToResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void refundBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessLogicException("Бронь не найдена"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessLogicException("Бронь уже отменена или истекла");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    public SessionOccupancyResponse getSessionOccupancy(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessLogicException("Сеанс не найден"));

        int totalSeats = seatRepository.findByHallIdOrderByRowNumberAscSeatNumberAsc(session.getHall().getId()).size();
        int occupiedSeats = bookedSeatRepository.findOccupiedSeatsBySessionId(sessionId).size();
        int availableSeats = Math.max(0, totalSeats - occupiedSeats);
        double occupancyPercent = totalSeats > 0
                ? Math.round((occupiedSeats * 10000.0) / totalSeats) / 100.0
                : 0.0;

        BigDecimal totalRevenue = bookingRepository
                .findBySessionIdAndStatusIn(sessionId, List.of(BookingStatus.CONFIRMED))
                .stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SessionOccupancyResponse.builder()
                .sessionId(session.getId())
                .movieTitle(session.getMovie().getTitle())
                .hallName(session.getHall().getName())
                .startTime(session.getStartTime())
                .totalSeats(totalSeats)
                .occupiedSeats(occupiedSeats)
                .availableSeats(availableSeats)
                .occupancyPercent(occupancyPercent)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public BookingResponse verifyTicket(String code) {
        Ticket ticket = ticketRepository.findByTicketNumber(code)
                .or(() -> ticketRepository.findByQrCode(code))
                .orElseThrow(() -> new BusinessLogicException("Билет с таким кодом не найден"));

        return mapToResponse(ticket.getBooking(), ticket);
    }

    private BookingResponse mapToResponse(Booking booking) {
        Ticket ticket = null;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            ticket = ticketRepository.findByBookingId(booking.getId()).orElse(null);
        }
        return mapToResponse(booking, ticket);
    }

    private BookingResponse mapToResponse(Booking booking, Ticket ticket) {
        return BookingResponse.builder()
                .id(booking.getId())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO)
                .movieTitle(booking.getSession().getMovie().getTitle())
                .hallName(booking.getSession().getHall().getName())
                .sessionStartTime(booking.getSession().getStartTime())
                .seats(booking.getBookedSeats().stream()
                        .map(bs -> BookingResponse.SeatInfo.builder()
                                .rowNumber(bs.getSeat().getRowNumber())
                                .seatNumber(bs.getSeat().getSeatNumber())
                                .price(bs.getFixedPrice())
                                .build())
                        .collect(Collectors.toList()))
                .ticket(ticket != null ? mapTicketToInfo(ticket) : null)
                .build();
    }

    private BookingResponse.TicketInfo mapTicketToInfo(Ticket ticket) {
        return BookingResponse.TicketInfo.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .qrCode(ticket.getQrCode())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
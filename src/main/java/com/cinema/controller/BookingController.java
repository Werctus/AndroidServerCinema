package com.cinema.controller;

import com.cinema.dto.request.BookingRequest;
import com.cinema.dto.response.BookingResponse;
import com.cinema.service.BookingService;
import com.cinema.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'CASHIER', 'ADMIN')")
public class BookingController {

    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    // Создание брони
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            HttpServletRequest request,
            @Valid @RequestBody BookingRequest dto) {

        // Извлекаем userId из токена
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);

        return ResponseEntity.ok(bookingService.createBooking(userId, dto));
    }

    // История бронирований текущего пользователя
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    // Подтверждение оплаты и генерация билета с QR-кодом
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirmPayment(id));
    }

    // Получение данных билета по ID бронирования
    @GetMapping("/{id}/ticket")
    public ResponseEntity<BookingResponse.TicketInfo> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getTicketByBookingId(id));
    }

    // Получение QR-кода билета в формате PNG
    @GetMapping(value = "/{id}/ticket/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getTicketQrCode(@PathVariable Long id) {
        byte[] qrImage = bookingService.getTicketQrCodeImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    // Отмена брони (только владелец или админ)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);

        bookingService.cancelBooking(id, userId);
        return ResponseEntity.noContent().build();
    }
}
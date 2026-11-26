package com.cinema.controller;

import com.cinema.dto.request.CashierSaleRequest;
import com.cinema.dto.response.BookingResponse;
import com.cinema.dto.response.SessionOccupancyResponse;
import com.cinema.service.BookingService;
import com.cinema.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cashier")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
public class CashierController {

    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    // Продажа билетов на кассе (сразу с подтверждением оплаты и опциональной скидкой)
    @PostMapping("/sell")
    public ResponseEntity<BookingResponse> sellTickets(
            HttpServletRequest request,
            @Valid @RequestBody CashierSaleRequest dto) {
        String token = request.getHeader("Authorization").substring(7);
        Long cashierId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(bookingService.sellTicketsAsCashier(cashierId, dto));
    }

    // Применение скидки к ожидающей оплате брони
    @PostMapping("/bookings/{id}/discount")
    public ResponseEntity<BookingResponse> applyDiscount(
            @PathVariable Long id,
            @RequestParam BigDecimal percent) {
        return ResponseEntity.ok(bookingService.applyDiscount(id, percent));
    }

    // Оформление возврата билета
    @PostMapping("/bookings/{id}/refund")
    public ResponseEntity<Void> refundBooking(@PathVariable Long id) {
        bookingService.refundBooking(id);
        return ResponseEntity.noContent().build();
    }

    // Просмотр информации о загрузке зала на сеанс
    @GetMapping("/sessions/{id}/occupancy")
    public ResponseEntity<SessionOccupancyResponse> getSessionOccupancy(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getSessionOccupancy(id));
    }

    // Проверка билета по номеру или QR-коду на входе/кассе
    @GetMapping("/tickets/verify")
    public ResponseEntity<BookingResponse> verifyTicket(@RequestParam String code) {
        return ResponseEntity.ok(bookingService.verifyTicket(code));
    }
}

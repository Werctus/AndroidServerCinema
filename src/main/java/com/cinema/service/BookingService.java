package com.cinema.service;

import com.cinema.dto.request.BookingRequest;
import com.cinema.dto.request.CashierSaleRequest;
import com.cinema.dto.response.BookingResponse;
import com.cinema.dto.response.SessionOccupancyResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BookingService {

    // Создание новой брони (пользователем или кассиром)
    BookingResponse createBooking(Long userId, BookingRequest request);

    // Подтверждение оплаты (переводит статус из PENDING в CONFIRMED)
    BookingResponse confirmPayment(Long bookingId);

    // Отмена/Возврат билета
    void cancelBooking(Long bookingId, Long userId);

    // Получение истории бронирований пользователя
    List<BookingResponse> getUserBookings(Long userId);

    // Проверка доступности мест
    boolean areSeatsAvailable(Long sessionId, List<Long> seatIds);

    // Получение билета по ID бронирования
    BookingResponse.TicketInfo getTicketByBookingId(Long bookingId);

    // Генерация изображения QR-кода билета (PNG)
    byte[] getTicketQrCodeImage(Long bookingId);

    // --- Операции кассира ---

    // Продажа билетов на кассе (сразу с подтверждением и опциональной скидкой)
    BookingResponse sellTicketsAsCashier(Long cashierId, CashierSaleRequest request);

    // Применение скидки к ожидающей оплате брони
    BookingResponse applyDiscount(Long bookingId, BigDecimal discountPercent);

    // Оформление возврата (отмена брони и освобождение мест)
    void refundBooking(Long bookingId);

    // Информация о загрузке зала для сеанса
    SessionOccupancyResponse getSessionOccupancy(Long sessionId);

    // Проверка билета по номеру или QR-коду
    BookingResponse verifyTicket(String code);
}
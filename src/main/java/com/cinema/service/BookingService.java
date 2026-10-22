package com.cinema.service;

import com.cinema.dto.request.BookingRequest;
import com.cinema.dto.response.BookingResponse;
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
}
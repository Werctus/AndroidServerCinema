package com.cinema.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull(message = "ID сеанса обязателен")
    private Long sessionId;

    @NotEmpty(message = "Выберите хотя бы одно место")
    private List<Long> seatIds; // ID мест из таблицы seats
}
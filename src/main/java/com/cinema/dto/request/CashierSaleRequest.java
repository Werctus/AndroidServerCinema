package com.cinema.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CashierSaleRequest {
    @NotNull(message = "ID сеанса обязателен")
    private Long sessionId;

    @NotEmpty(message = "Выберите хотя бы одно место")
    private List<Long> seatIds;

    @DecimalMin(value = "0.0", message = "Скидка не может быть отрицательной")
    @DecimalMax(value = "50.0", message = "Максимальная скидка — 50%")
    private BigDecimal discountPercent = BigDecimal.ZERO;

    private String customerEmail;
}

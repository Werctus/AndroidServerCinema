package com.cinema.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SessionRequest {
    @NotNull(message = "ID фильма обязателен")
    private Long movieId;

    @NotNull(message = "ID зала обязателен")
    private Long hallId;

    @NotNull(message = "Время начала обязательно")
    @Future(message = "Сеанс должен быть в будущем")
    private LocalDateTime startTime;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
    private BigDecimal price;

    private String format; // 2D, 3D, IMAX
}
package com.cinema.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private String movieTitle;
    private String hallName;
    private LocalDateTime startTime; // Начало сеанса
    private BigDecimal price;
    private String format;
    private Integer duration; // Нужно для расчета конца сеанса
}
package com.cinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionOccupancyResponse {
    private Long sessionId;
    private String movieTitle;
    private String hallName;
    private LocalDateTime startTime;
    private int totalSeats;
    private int occupiedSeats;
    private int availableSeats;
    private double occupancyPercent;
    private BigDecimal totalRevenue;
}

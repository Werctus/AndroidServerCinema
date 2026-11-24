package com.cinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSeatsResponse {
    private Long sessionId;
    private Long movieId;
    private String movieTitle;
    private Long hallId;
    private String hallName;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private String format;
    private Integer totalRows;
    private Integer totalSeats;
    private List<SeatDto> seats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatDto {
        private Long id;
        private Integer rowNumber;
        private Integer seatNumber;
        private String seatType; // STANDARD, VIP
        private BigDecimal price;
        private boolean isAvailable; // true = свободно, false = занято/забронировано
    }
}

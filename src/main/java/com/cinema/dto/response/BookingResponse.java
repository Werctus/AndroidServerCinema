package com.cinema.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private List<SeatInfo> seats;

    @Data
    @Builder
    public static class SeatInfo {
        private Integer rowNumber;
        private Integer seatNumber;
        private BigDecimal price;
    }
}
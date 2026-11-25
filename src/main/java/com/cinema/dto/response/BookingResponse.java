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
    private String movieTitle;
    private String hallName;
    private LocalDateTime sessionStartTime;
    private List<SeatInfo> seats;
    private TicketInfo ticket;

    @Data
    @Builder
    public static class SeatInfo {
        private Integer rowNumber;
        private Integer seatNumber;
        private BigDecimal price;
    }

    @Data
    @Builder
    public static class TicketInfo {
        private Long id;
        private String ticketNumber;
        private String qrCode;
        private LocalDateTime createdAt;
    }
}
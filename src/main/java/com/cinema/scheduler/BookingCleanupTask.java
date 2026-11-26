package com.cinema.scheduler;

import com.cinema.entity.Booking;
import com.cinema.entity.BookingStatus;
import com.cinema.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupTask {

    private static final int BOOKING_TTL_MINUTES = 15;
    private final BookingRepository bookingRepository;

    /**
     * Каждую минуту переводит неоплаченные брони старше 15 минут в статус EXPIRED,
     * автоматически освобождая места для других зрителей.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireUnpaidBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(BOOKING_TTL_MINUTES);
        List<Booking> expired = bookingRepository.findExpiredPendingBookings(cutoff);

        if (!expired.isEmpty()) {
            for (Booking booking : expired) {
                booking.setStatus(BookingStatus.EXPIRED);
            }
            bookingRepository.saveAll(expired);
            log.info("Истёк срок оплаты для {} бронирований, места освобождены.", expired.size());
        }
    }
}

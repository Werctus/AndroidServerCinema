package com.cinema.repository;

import com.cinema.entity.Booking;
import com.cinema.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // История бронирований пользователя (сортировка по дате создания)
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Активные брони (PENDING или CONFIRMED)
    List<Booking> findBySessionIdAndStatusIn(Long sessionId, List<BookingStatus> statuses);

    //  Выборка продаж за период
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.createdAt BETWEEN :start AND :end")
    List<Booking> findConfirmedBookingsByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
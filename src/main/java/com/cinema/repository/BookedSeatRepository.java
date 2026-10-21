package com.cinema.repository;

import com.cinema.entity.BookedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat, Long> {

    @Query("SELECT bs FROM BookedSeat bs JOIN bs.booking b WHERE b.session.id = :sessionId AND b.status IN ('CONFIRMED', 'PENDING')")
    List<BookedSeat> findOccupiedSeatsBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT COUNT(bs) > 0 FROM BookedSeat bs JOIN bs.booking b WHERE b.session.id = :sessionId AND bs.seat.id = :seatId AND b.status IN ('CONFIRMED', 'PENDING')")
    boolean isSeatOccupied(@Param("sessionId") Long sessionId, @Param("seatId") Long seatId);
}
package com.cinema.repository;

import com.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByHallIdOrderByRowNumberAscSeatNumberAsc(Long hallId);
    Seat findByHallIdAndRowNumberAndSeatNumber(Long hallId, Integer rowNumber, Integer seatNumber);
}
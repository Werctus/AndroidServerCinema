package com.cinema.repository;

import com.cinema.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    // Поиск сеансов конкретного фильма
    List<Session> findByMovieId(Long movieId);

    // Поиск сеансов в конкретном зале
    List<Session> findByHallId(Long hallId);

    // Проверка пересечения времени в том же зале
    // Возвращает список сеансов, которые пересекаются с указанным временем
    @Query("SELECT s FROM Session s WHERE s.hall.id = :hallId " +
            "AND s.startTime < :endTime AND s.startTime + CAST(s.movie.duration AS duration) > :startTime")
    List<Session> findOverlappingSessions(
            @Param("hallId") Long hallId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Поиск сеансов для афиши на определенную дату
    @Query("SELECT s FROM Session s WHERE FUNCTION('DATE', s.startTime) = :date")
    List<Session> findByDate(@Param("date") java.time.LocalDate date);
}
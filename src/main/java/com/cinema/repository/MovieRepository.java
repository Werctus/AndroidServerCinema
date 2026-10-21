package com.cinema.repository;

import com.cinema.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Derived Query: поиск по подстроке названия
    // Поиск по названию
    List<Movie> findByTitleContainingIgnoreCase(String title);

    // Derived Query: поиск по названию жанра (через коллекцию genres)
    // Поиск фильмов по жанрам
    List<Movie> findByGenres_Name(String genreName);

    // Поиск активных фильмов в прокате
    @Query("SELECT m FROM Movie m WHERE m.releaseDate <= :date AND (m.endDate IS NULL OR m.endDate >= :date)")
    List<Movie> findActiveMovies(@Param("date") LocalDate date);
}
package com.cinema.repository;

import com.cinema.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    // Поиск жанра по названию
    List<Genre> findByNameContainingIgnoreCase(String name);
}
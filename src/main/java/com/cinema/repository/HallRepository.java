package com.cinema.repository;

import com.cinema.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {

    Optional<Hall> findByName(String name);

    // Получить только активные залы
    List<Hall> findByIsActiveTrue();
}
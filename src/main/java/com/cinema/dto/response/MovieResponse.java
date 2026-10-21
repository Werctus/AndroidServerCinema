package com.cinema.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private Integer duration;
    private String ageRating;
    private LocalDate releaseDate;
    private String posterUrl;
    private Set<String> genres; // Отдаем только названия жанров
}
package com.cinema.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

@Data
public class MovieRequest {
    @NotBlank(message = "Название обязательно")
    private String title;

    @NotNull(message = "Длительность обязательна")
    @Min(value = 1, message = "Длительность должна быть положительной")
    private Integer duration;

    private String ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String description;
    private String posterUrl;

    // ID жанров, которые нужно привязать
    private Set<Long> genreIds;
}
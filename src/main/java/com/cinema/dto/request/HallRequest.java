package com.cinema.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class HallRequest {
    @NotBlank(message = "Название зала обязательно")
    private String name;

    @NotNull(message = "Количество рядов обязательно")
    @Min(value = 1, message = "Должен быть хотя бы 1 ряд")
    private Integer rowsCount;

    @NotNull(message = "Количество мест в ряду обязательно")
    @Min(value = 1, message = "В ряду должно быть хотя бы 1 место")
    private Integer seatsPerRow;

    private String description;

    // Номера рядов с VIP-местами (необязательно)
    private Set<Integer> vipRows;
}

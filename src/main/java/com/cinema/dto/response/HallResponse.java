package com.cinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HallResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String description;
    private Boolean isActive;
}

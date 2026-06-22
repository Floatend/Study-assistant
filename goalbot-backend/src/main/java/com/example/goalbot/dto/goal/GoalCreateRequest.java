package com.example.goalbot.dto.goal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GoalCreateRequest {

    @NotBlank
    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    @Max(4)
    private Integer priority = 2;

    @Min(0)
    @Max(4)
    private Integer status = 0;

}

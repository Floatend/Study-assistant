package com.example.goalbot.dto.checkin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckinCreateRequest {

    @NotNull
    private Long taskId;

    @NotNull
    @Min(0)
    private Integer actualMinutes;

    private String content;

    @Min(1)
    @Max(5)
    private Integer mood;

    @Min(1)
    @Max(5)
    private Integer difficulty;
}

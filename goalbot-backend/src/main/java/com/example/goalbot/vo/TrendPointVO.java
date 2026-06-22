package com.example.goalbot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {

    private LocalDate date;

    private Integer minutes;

    private Integer completedTasks;
}

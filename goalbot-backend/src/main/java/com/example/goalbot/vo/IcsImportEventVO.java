package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class IcsImportEventVO {

    private String uid;

    private String title;

    private String description;

    private String location;

    private LocalDate planDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer plannedMinutes;

    private Boolean allDay;

    private Boolean skipped;

    private String skipReason;
}

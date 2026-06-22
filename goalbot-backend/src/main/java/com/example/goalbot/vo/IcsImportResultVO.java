package com.example.goalbot.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IcsImportResultVO {

    private Integer sourceEventCount = 0;

    private Integer expandedEventCount = 0;

    private Integer importedCount = 0;

    private Integer skippedCount = 0;

    private Boolean dryRun = true;

    private List<IcsImportEventVO> events = new ArrayList<>();

    private List<TaskVO> importedTasks = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();
}

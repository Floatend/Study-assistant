package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("conversation_task_draft")
public class ConversationTaskDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    private String title;

    private String description;

    private LocalDate planDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer plannedMinutes;

    private Long goalId;

    private String goalKeyword;

    private String missingSlots;

    private Integer status;

    private String sourceText;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

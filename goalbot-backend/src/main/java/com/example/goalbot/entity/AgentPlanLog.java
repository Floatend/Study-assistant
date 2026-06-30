package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_plan_log")
public class AgentPlanLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String messageId;
    private String runMode;
    private Integer selected;
    private String planMode;
    private Double confidence;
    private String primaryTool;
    private String planJson;
    private String errorMessage;
    private LocalDateTime createdAt;
}

package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("command_log")
public class CommandLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String feishuMessageId;

    private String rawText;

    private String intent;

    private String taskKeyword;

    private Integer actualMinutes;

    private String source;

    private Integer success;

    private String errorMessage;

    private String replyContent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

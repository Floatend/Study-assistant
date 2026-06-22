package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_session")
public class ConversationSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String channel;

    private Integer status;

    private String topic;

    private String state;

    private String lastIntent;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

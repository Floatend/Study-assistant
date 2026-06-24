package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_transition_log")
public class ConversationTransitionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long userId;
    private Long draftId;
    private String transitionType;
    private String rawText;
    private String stateBefore;
    private String semanticFrame;
    private String stateAfter;
    private String decision;
    private String clarificationQuestion;
    private LocalDateTime createdAt;
}

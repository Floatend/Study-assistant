package com.example.goalbot.agent.dialogue;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskDraftTransition {

    private String rawText;
    private TaskDraftSnapshot before;
    private TaskDraftFrame frame;
    private TaskDraftSnapshot after;
    private TaskDraftDecision decision;
    private String clarificationQuestion;
}

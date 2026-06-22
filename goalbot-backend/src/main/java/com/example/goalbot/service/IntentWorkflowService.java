package com.example.goalbot.service;

import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.vo.GoalVO;
import com.example.goalbot.vo.TaskVO;

import java.util.List;

public interface IntentWorkflowService {

    CommandIntent parseIntent(Long userId, String text, List<TaskVO> todayTasks, List<GoalVO> goals);
}

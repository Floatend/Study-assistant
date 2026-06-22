package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.goal.GoalCreateRequest;
import com.example.goalbot.dto.goal.GoalUpdateRequest;
import com.example.goalbot.entity.Goal;
import com.example.goalbot.vo.GoalVO;

import java.util.List;

public interface GoalService extends IService<Goal> {

    GoalVO createGoal(Long userId, GoalCreateRequest request);

    List<GoalVO> listGoals(Long userId, Integer status, Integer priority, String keyword);

    GoalVO getGoal(Long userId, Long id);

    Goal getOwnedGoalEntity(Long userId, Long id);

    GoalVO updateGoal(Long userId, Long id, GoalUpdateRequest request);

    void deleteGoal(Long userId, Long id);

    void refreshGoalTaskState(Long goalId);
}

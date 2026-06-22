package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.dto.goal.GoalCreateRequest;
import com.example.goalbot.dto.goal.GoalUpdateRequest;
import com.example.goalbot.entity.Goal;
import com.example.goalbot.entity.Task;
import com.example.goalbot.mapper.GoalMapper;
import com.example.goalbot.mapper.TaskMapper;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.vo.GoalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl extends ServiceImpl<GoalMapper, Goal> implements GoalService {

    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public GoalVO createGoal(Long userId, GoalCreateRequest request) {
        Goal goal = new Goal();
        BeanUtils.copyProperties(request, goal);
        goal.setUserId(userId);
        if (goal.getPriority() == null) {
            goal.setPriority(2);
        }
        if (goal.getStatus() == null) {
            goal.setStatus(0);
        }
        if (goal.getProgress() == null) {
            goal.setProgress(BigDecimal.ZERO);
        }
        normalizeCompletedGoal(goal);
        save(goal);
        return toVO(goal);
    }

    @Override
    public List<GoalVO> listGoals(Long userId, Integer status, Integer priority, String keyword) {
        LambdaQueryWrapper<Goal> wrapper = new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId)
                .eq(status != null, Goal::getStatus, status)
                .eq(priority != null, Goal::getPriority, priority)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(Goal::getTitle, keyword)
                        .or()
                        .like(Goal::getDescription, keyword))
                .orderByAsc(Goal::getStatus)
                .orderByDesc(Goal::getPriority)
                .orderByDesc(Goal::getCreatedAt);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public GoalVO getGoal(Long userId, Long id) {
        return toVO(getOwnedGoalEntity(userId, id));
    }

    @Override
    public Goal getOwnedGoalEntity(Long userId, Long id) {
        Goal goal = getOne(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getId, id)
                .eq(Goal::getUserId, userId));
        if (goal == null) {
            throw BusinessException.notFound("Goal not found");
        }
        return goal;
    }

    @Override
    @Transactional
    public GoalVO updateGoal(Long userId, Long id, GoalUpdateRequest request) {
        Goal goal = getOwnedGoalEntity(userId, id);
        if (StringUtils.hasText(request.getTitle())) {
            goal.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            goal.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            goal.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            goal.setEndDate(request.getEndDate());
        }
        if (request.getPriority() != null) {
            goal.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }
        normalizeCompletedGoal(goal);
        updateById(goal);
        return toVO(getById(id));
    }

    @Override
    @Transactional
    public void deleteGoal(Long userId, Long id) {
        Goal goal = getOwnedGoalEntity(userId, id);
        removeById(goal.getId());
    }

    @Override
    @Transactional
    public void refreshGoalTaskState(Long goalId) {
        if (goalId == null) {
            return;
        }
        Goal goal = getById(goalId);
        if (goal == null) {
            return;
        }
        if (Objects.equals(goal.getStatus(), 3)) {
            goal.setProgress(BigDecimal.valueOf(100));
            updateById(goal);
            return;
        }

        Long total = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getGoalId, goalId)
                .in(Task::getStatus, List.of(0, 2)));
        Long completed = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getGoalId, goalId)
                .eq(Task::getStatus, 2));

        BigDecimal progress = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        goal.setProgress(progress);

        if (progress.compareTo(BigDecimal.valueOf(100)) == 0) {
            goal.setStatus(3);
        } else if (progress.compareTo(BigDecimal.ZERO) > 0 && goal.getStatus() == 0) {
            goal.setStatus(1);
        }
        updateById(goal);
    }

    private GoalVO toVO(Goal goal) {
        GoalVO vo = new GoalVO();
        BeanUtils.copyProperties(goal, vo);
        vo.setTotalTaskCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getGoalId, goal.getId())
                .in(Task::getStatus, List.of(0, 2))));
        vo.setCompletedTaskCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getGoalId, goal.getId())
                .eq(Task::getStatus, 2)));
        return vo;
    }

    private void normalizeCompletedGoal(Goal goal) {
        if (Objects.equals(goal.getStatus(), 3)) {
            goal.setProgress(BigDecimal.valueOf(100));
        }
    }
}

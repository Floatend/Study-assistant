package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.dto.task.TaskCreateRequest;
import com.example.goalbot.dto.task.TaskUpdateRequest;
import com.example.goalbot.entity.Goal;
import com.example.goalbot.entity.Task;
import com.example.goalbot.mapper.GoalMapper;
import com.example.goalbot.mapper.TaskMapper;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private static final String ICS_SOURCE_MARKER = "来源：ICS 导入";

    private final GoalMapper goalMapper;
    private final GoalService goalService;

    @Override
    @Transactional
    public TaskVO createTask(Long userId, TaskCreateRequest request) {
        validateGoal(userId, request.getGoalId());
        Task task = new Task();
        BeanUtils.copyProperties(request, task);
        task.setUserId(userId);
        task.setStatus(normalizeTaskStatus(task.getStatus()));
        if (task.getPlannedMinutes() == null) {
            task.setPlannedMinutes(0);
        }
        save(task);
        goalService.refreshGoalTaskState(task.getGoalId());
        return toVO(task);
    }

    @Override
    public List<TaskVO> listTasks(Long userId, LocalDate date, Long goalId, Integer status) {
        Integer normalizedStatus = normalizeQueryStatus(status);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(date != null, Task::getPlanDate, date)
                .eq(goalId != null, Task::getGoalId, goalId)
                .eq(normalizedStatus != null, Task::getStatus, normalizedStatus)
                .in(normalizedStatus == null, Task::getStatus, List.of(0, 2))
                .orderByAsc(Task::getPlanDate)
                .orderByAsc(Task::getStartTime)
                .orderByDesc(Task::getCreatedAt);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<TaskVO> listTodayTasks(Long userId) {
        return listActiveTasksByDate(userId, LocalDate.now());
    }

    @Override
    public List<TaskVO> listActiveTasksByDate(Long userId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getPlanDate, targetDate)
                .in(Task::getStatus, List.of(0, 2))
                .orderByAsc(Task::getPlanDate)
                .orderByAsc(Task::getStartTime)
                .orderByDesc(Task::getCreatedAt);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<TaskVO> listActiveCalendarTasks(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .ge(startDate != null, Task::getPlanDate, startDate)
                .le(endDate != null, Task::getPlanDate, endDate)
                .in(Task::getStatus, List.of(0, 2))
                .orderByAsc(Task::getPlanDate)
                .orderByAsc(Task::getStartTime);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<TaskVO> listCalendarTasks(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .ge(startDate != null, Task::getPlanDate, startDate)
                .le(endDate != null, Task::getPlanDate, endDate)
                .in(Task::getStatus, List.of(0, 2))
                .orderByAsc(Task::getPlanDate)
                .orderByAsc(Task::getStartTime);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public TaskVO updateTask(Long userId, Long id, TaskUpdateRequest request) {
        Task task = getOwnedTaskEntity(userId, id);
        Long oldGoalId = task.getGoalId();

        if (request.getGoalId() != null) {
            validateGoal(userId, request.getGoalId());
            task.setGoalId(request.getGoalId());
        }
        if (StringUtils.hasText(request.getTitle())) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPlanDate() != null) {
            task.setPlanDate(request.getPlanDate());
        }
        if (request.getStartTime() != null) {
            task.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            task.setEndTime(request.getEndTime());
        }
        if (request.getPlannedMinutes() != null) {
            task.setPlannedMinutes(request.getPlannedMinutes());
        }
        if (request.getStatus() != null) {
            task.setStatus(normalizeTaskStatus(request.getStatus()));
        }
        updateById(task);
        goalService.refreshGoalTaskState(oldGoalId);
        goalService.refreshGoalTaskState(task.getGoalId());
        return toVO(getById(id));
    }

    @Override
    @Transactional
    public TaskVO completeTask(Long userId, Long id) {
        Task task = getOwnedTaskEntity(userId, id);
        task.setStatus(2);
        updateById(task);
        goalService.refreshGoalTaskState(task.getGoalId());
        return toVO(task);
    }

    @Override
    @Transactional
    public List<TaskVO> cancelTasksByDate(Long userId, LocalDate date) {
        if (date == null) {
            throw BusinessException.badRequest("Task deletion date is required");
        }
        return deletePendingTasks(userId, date, date, null, false);
    }

    @Override
    @Transactional
    public List<TaskVO> deletePendingTasksInRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String titleKeyword
    ) {
        validateDeletionRange(startDate, endDate, "Task");
        return deletePendingTasks(userId, startDate, endDate, titleKeyword, false);
    }

    @Override
    @Transactional
    public List<TaskVO> deletePendingIcsTasks(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String titleKeyword
    ) {
        validateDeletionRange(startDate, endDate, "Imported schedule");
        return deletePendingTasks(userId, startDate, endDate, titleKeyword, true);
    }

    private List<TaskVO> deletePendingTasks(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String titleKeyword,
            boolean icsOnly
    ) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, 0)
                .ge(Task::getPlanDate, startDate)
                .le(Task::getPlanDate, endDate)
                .like(icsOnly, Task::getDescription, ICS_SOURCE_MARKER)
                .like(StringUtils.hasText(titleKeyword), Task::getTitle, titleKeyword)
                .orderByAsc(Task::getPlanDate)
                .orderByAsc(Task::getStartTime)
                .orderByDesc(Task::getCreatedAt);
        List<Task> tasks = list(wrapper);
        if (tasks.isEmpty()) {
            return List.of();
        }

        removeByIds(tasks.stream().map(Task::getId).toList());
        tasks.stream()
                .map(Task::getGoalId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(goalService::refreshGoalTaskState);
        return tasks.stream().map(this::toVO).toList();
    }

    private void validateDeletionRange(LocalDate startDate, LocalDate endDate, String label) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw BusinessException.badRequest("Invalid " + label.toLowerCase() + " deletion date range");
        }
        if (startDate.plusDays(31).isBefore(endDate)) {
            throw BusinessException.badRequest(label + " deletion range cannot exceed 31 days");
        }
    }

    @Override
    @Transactional
    public void deleteTask(Long userId, Long id) {
        Task task = getOwnedTaskEntity(userId, id);
        Long oldGoalId = task.getGoalId();
        removeById(task.getId());
        goalService.refreshGoalTaskState(oldGoalId);
    }

    @Override
    public Task getOwnedTaskEntity(Long userId, Long id) {
        Task task = getOne(new LambdaQueryWrapper<Task>()
                .eq(Task::getId, id)
                .eq(Task::getUserId, userId));
        if (task == null) {
            throw BusinessException.notFound("Task not found");
        }
        return task;
    }

    private void validateGoal(Long userId, Long goalId) {
        if (goalId == null) {
            return;
        }
        Long count = goalMapper.selectCount(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getId, goalId)
                .eq(Goal::getUserId, userId));
        if (count == 0) {
            throw BusinessException.badRequest("Goal does not belong to current user");
        }
    }

    private Integer normalizeQueryStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return normalizeTaskStatus(status);
    }

    private Integer normalizeTaskStatus(Integer status) {
        if (status == null || Objects.equals(status, 0)) {
            return 0;
        }
        if (Objects.equals(status, 2)) {
            return 2;
        }
        throw BusinessException.badRequest("Task status only supports 0 pending and 2 completed");
    }

    private TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        if (task.getGoalId() != null) {
            Goal goal = goalMapper.selectById(task.getGoalId());
            if (goal != null) {
                vo.setGoalTitle(goal.getTitle());
            }
        }
        return vo;
    }
}

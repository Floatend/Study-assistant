package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.task.TaskCreateRequest;
import com.example.goalbot.dto.task.TaskUpdateRequest;
import com.example.goalbot.entity.Task;
import com.example.goalbot.vo.TaskVO;

import java.time.LocalDate;
import java.util.List;

public interface TaskService extends IService<Task> {

    TaskVO createTask(Long userId, TaskCreateRequest request);

    List<TaskVO> listTasks(Long userId, LocalDate date, Long goalId, Integer status);

    List<TaskVO> listTodayTasks(Long userId);

    List<TaskVO> listActiveTasksByDate(Long userId, LocalDate date);

    List<TaskVO> listActiveCalendarTasks(Long userId, LocalDate startDate, LocalDate endDate);

    List<TaskVO> listCalendarTasks(Long userId, LocalDate startDate, LocalDate endDate);

    TaskVO updateTask(Long userId, Long id, TaskUpdateRequest request);

    TaskVO completeTask(Long userId, Long id);

    List<TaskVO> cancelTasksByDate(Long userId, LocalDate date);

    List<TaskVO> deletePendingTasksInRange(Long userId, LocalDate startDate, LocalDate endDate, String titleKeyword);

    List<TaskVO> deletePendingIcsTasks(Long userId, LocalDate startDate, LocalDate endDate, String titleKeyword);

    void deleteTask(Long userId, Long id);

    Task getOwnedTaskEntity(Long userId, Long id);
}

package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.dto.checkin.CheckinCreateRequest;
import com.example.goalbot.entity.Checkin;
import com.example.goalbot.entity.Task;
import com.example.goalbot.mapper.CheckinMapper;
import com.example.goalbot.mapper.TaskMapper;
import com.example.goalbot.service.CheckinService;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.vo.CheckinStatsVO;
import com.example.goalbot.vo.CheckinVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, Checkin> implements CheckinService {

    private final TaskMapper taskMapper;
    private final GoalService goalService;

    @Override
    @Transactional
    public CheckinVO createCheckin(Long userId, CheckinCreateRequest request) {
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>()
                .eq(Task::getId, request.getTaskId())
                .eq(Task::getUserId, userId));
        if (task == null) {
            throw com.example.goalbot.common.BusinessException.notFound("Task not found");
        }

        Checkin checkin = new Checkin();
        BeanUtils.copyProperties(request, checkin);
        checkin.setUserId(userId);
        checkin.setActualMinutes(request.getActualMinutes() == null
                ? zero(task.getPlannedMinutes())
                : request.getActualMinutes());
        save(checkin);

        if (task.getStatus() != 3) {
            task.setStatus(2);
            taskMapper.updateById(task);
            goalService.refreshGoalTaskState(task.getGoalId());
        }
        return toVO(checkin);
    }

    private int zero(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    @Override
    public List<CheckinVO> listRecent(Long userId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        return list(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCreatedAt)
                .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CheckinStatsVO getStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(6) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1).atStartOfDay();

        List<Checkin> checkins = list(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .ge(Checkin::getCreatedAt, startTime)
                .lt(Checkin::getCreatedAt, endExclusive));

        CheckinStatsVO stats = new CheckinStatsVO();
        stats.setCheckinCount(checkins.size());
        stats.setTotalMinutes(checkins.stream().mapToInt(c -> c.getActualMinutes() == null ? 0 : c.getActualMinutes()).sum());
        stats.setCompletedTaskCount((int) checkins.stream().map(Checkin::getTaskId).distinct().count());
        stats.setAverageMood(checkins.stream()
                .filter(c -> c.getMood() != null)
                .mapToInt(Checkin::getMood)
                .average()
                .orElse(0));
        stats.setAverageDifficulty(checkins.stream()
                .filter(c -> c.getDifficulty() != null)
                .mapToInt(Checkin::getDifficulty)
                .average()
                .orElse(0));
        return stats;
    }

    private CheckinVO toVO(Checkin checkin) {
        CheckinVO vo = new CheckinVO();
        BeanUtils.copyProperties(checkin, vo);
        if (checkin.getTaskId() != null) {
            Task task = taskMapper.selectById(checkin.getTaskId());
            if (task != null) {
                vo.setTaskTitle(task.getTitle());
            }
        }
        return vo;
    }
}

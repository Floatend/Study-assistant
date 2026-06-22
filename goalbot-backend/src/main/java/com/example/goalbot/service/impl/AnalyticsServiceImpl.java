package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.entity.Checkin;
import com.example.goalbot.entity.Task;
import com.example.goalbot.mapper.CheckinMapper;
import com.example.goalbot.mapper.TaskMapper;
import com.example.goalbot.service.AnalyticsService;
import com.example.goalbot.vo.TaskStatusCountVO;
import com.example.goalbot.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CheckinMapper checkinMapper;
    private final TaskMapper taskMapper;

    @Override
    public List<TrendPointVO> getStudyDuration(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(13) : startDate;
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1).atStartOfDay();

        Map<LocalDate, Integer> minutesByDate = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getUserId, userId)
                        .ge(Checkin::getCreatedAt, startTime)
                        .lt(Checkin::getCreatedAt, endExclusive))
                .stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate(),
                        Collectors.summingInt(c -> c.getActualMinutes() == null ? 0 : c.getActualMinutes())
                ));

        Map<LocalDate, Integer> completedByDate = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .eq(Task::getStatus, 2)
                        .ge(Task::getPlanDate, start)
                        .le(Task::getPlanDate, end))
                .stream()
                .collect(Collectors.groupingBy(
                        Task::getPlanDate,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return start.datesUntil(end.plusDays(1))
                .map(date -> new TrendPointVO(
                        date,
                        minutesByDate.getOrDefault(date, 0),
                        completedByDate.getOrDefault(date, 0)
                ))
                .toList();
    }

    @Override
    public List<TaskStatusCountVO> getTaskStatus(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(13) : startDate;
        Map<Integer, Long> counts = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .in(Task::getStatus, List.of(0, 2))
                        .ge(Task::getPlanDate, start)
                        .le(Task::getPlanDate, end))
                .stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

        return List.of(0, 2)
                .stream()
                .map(status -> new TaskStatusCountVO(status, counts.getOrDefault(status, 0L)))
                .toList();
    }
}

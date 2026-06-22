package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.common.AdviceSourceHasher;
import com.example.goalbot.entity.Checkin;
import com.example.goalbot.entity.Review;
import com.example.goalbot.mapper.CheckinMapper;
import com.example.goalbot.mapper.ReviewMapper;
import com.example.goalbot.service.DashboardService;
import com.example.goalbot.service.DifyService;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.DashboardVO;
import com.example.goalbot.vo.GoalVO;
import com.example.goalbot.vo.ReviewVO;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TaskService taskService;
    private final GoalService goalService;
    private final DifyService difyService;
    private final CheckinMapper checkinMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public DashboardVO getDashboard(Long userId) {
        return getDashboard(userId, 2);
    }

    @Override
    public DashboardVO getDashboard(Long userId, Integer adviceDays) {
        int normalizedAdviceDays = normalizeAdviceDays(adviceDays);
        LocalDate today = LocalDate.now();
        LocalDate adviceEndDate = today.plusDays(normalizedAdviceDays - 1L);
        List<TaskVO> todayTasks = taskService.listTodayTasks(userId);
        List<TaskVO> adviceTasks = taskService.listActiveCalendarTasks(userId, today, adviceEndDate);
        int total = todayTasks.size();
        int completed = (int) todayTasks.stream().filter(task -> task.getStatus() != null && task.getStatus() == 2).count();
        int actualMinutes = sumMinutes(userId, today, today);
        AdviceState adviceState = getAdviceState(userId, today, adviceEndDate, adviceTasks);

        DashboardVO vo = new DashboardVO();
        vo.setTodayTasks(todayTasks);
        vo.setTodayTaskCount(total);
        vo.setCompletedTaskCount(completed);
        vo.setTodayActualMinutes(actualMinutes);
        vo.setAdviceDays(normalizedAdviceDays);
        vo.setAdviceStartDate(today);
        vo.setAdviceEndDate(adviceEndDate);
        vo.setLatestAiAdvice(adviceState.content());
        vo.setAiAdviceStale(adviceState.stale());
        vo.setAiAdviceSourceHash(adviceState.sourceHash());
        return vo;
    }

    @Override
    public ReviewVO refreshTodayAdvice(Long userId) {
        return refreshAdvice(userId, 2);
    }

    @Override
    public ReviewVO refreshAdvice(Long userId, Integer adviceDays) {
        return difyService.generateAdvice(userId, normalizeAdviceDays(adviceDays));
    }

    private AdviceState getAdviceState(Long userId, LocalDate today, LocalDate adviceEndDate, List<TaskVO> adviceTasks) {
        List<GoalVO> goals = goalService.listGoals(userId, null, null, null);
        String currentHash = AdviceSourceHasher.adviceSourceHash(adviceTasks, goals, today, adviceEndDate);
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getReviewDate, today)
                .eq(Review::getType, 4)
                .last("LIMIT 1"));

        boolean stale = review == null || !Objects.equals(AdviceSourceHasher.extractHash(review.getSummary()), currentHash);
        String content = review == null || review.getAiAdvice() == null ? "" : review.getAiAdvice();
        return new AdviceState(content, stale, currentHash);
    }

    private int normalizeAdviceDays(Integer days) {
        if (days == null) {
            return 2;
        }
        return Math.max(1, Math.min(3, days));
    }

    private int sumMinutes(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getUserId, userId)
                        .ge(Checkin::getCreatedAt, startTime)
                        .lt(Checkin::getCreatedAt, endExclusive))
                .stream()
                .mapToInt(c -> c.getActualMinutes() == null ? 0 : c.getActualMinutes())
                .sum();
    }

    private record AdviceState(String content, boolean stale, String sourceHash) {
    }
}

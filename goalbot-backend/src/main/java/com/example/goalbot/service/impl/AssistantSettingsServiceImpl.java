package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.config.ReminderProperties;
import com.example.goalbot.dto.settings.AssistantSettingsUpdateRequest;
import com.example.goalbot.entity.AssistantSettings;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.entity.User;
import com.example.goalbot.integration.dify.DifyClient;
import com.example.goalbot.integration.feishu.FeishuOpenApiClient;
import com.example.goalbot.integration.feishu.FeishuProperties;
import com.example.goalbot.mapper.AssistantSettingsMapper;
import com.example.goalbot.mapper.NotificationMapper;
import com.example.goalbot.mapper.UserMapper;
import com.example.goalbot.service.AiBriefingService;
import com.example.goalbot.service.AssistantSettingsService;
import com.example.goalbot.service.NotificationService;
import com.example.goalbot.vo.AssistantSettingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantSettingsServiceImpl extends ServiceImpl<AssistantSettingsMapper, AssistantSettings>
        implements AssistantSettingsService {

    private static final String DAILY_TASK_TITLE = "今日任务提醒";
    private static final String DAILY_REVIEW_TITLE = "每日复盘提醒";
    private static final String WEEKLY_REVIEW_TITLE = "每周周报提醒";
    private static final String PERIODIC_NUDGE_TITLE = "主动规划提醒";
    private static final String AI_BRIEFING_TITLE = "每日 AI 资讯";

    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final AiBriefingService aiBriefingService;
    private final FeishuOpenApiClient feishuOpenApiClient;
    private final FeishuProperties feishuProperties;
    private final DifyClient difyClient;
    private final ReminderProperties reminderProperties;

    @Override
    public AssistantSettingsVO getSettings(Long userId) {
        return toVO(getOrCreate(userId));
    }

    @Override
    @Transactional
    public AssistantSettingsVO updateSettings(Long userId, AssistantSettingsUpdateRequest request) {
        AssistantSettings settings = getOrCreate(userId);
        applyUpdates(settings, request);
        updateById(settings);
        return toVO(getById(settings.getId()));
    }

    @Override
    public Notification sendTestMessage(Long userId) {
        AssistantSettings settings = getOrCreate(userId);
        String content = """
                这是一条 GoalBot 主动消息测试。
                如果你能在飞书里看到它，说明应用机器人主动推送链路已经打通。
                后续早间任务提醒、主动规划提醒、晚间复盘提醒、周报提醒和 AI 资讯都会走这条链路。
                """;
        return notificationService.sendCustomMessage(userId, "GoalBot 主动消息测试", content, settings.getFeishuChatId());
    }

    @Override
    public Notification sendNow(Long userId, String type) {
        AssistantSettings settings = getOrCreate(userId);
        return switch (type) {
            case "daily-task" -> notificationService.sendDailyTaskReminder(userId, settings.getFeishuChatId());
            case "daily-review" -> notificationService.sendDailyReviewReminder(userId, settings.getFeishuChatId());
            case "weekly-review" -> notificationService.sendWeeklyReviewReminder(userId, settings.getFeishuChatId());
            case "periodic-nudge" -> notificationService.sendPeriodicPlanningNudge(userId, settings.getFeishuChatId());
            case "ai-briefing" -> aiBriefingService.sendDailyBriefing(userId, settings);
            default -> throw BusinessException.badRequest("Unsupported proactive message type: " + type);
        };
    }

    @Override
    public List<Notification> sendDueProactiveMessages(LocalDateTime now) {
        LocalDateTime current = now == null ? LocalDateTime.now() : now;
        List<Notification> sent = new ArrayList<>();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1));
        for (User user : users) {
            AssistantSettings settings = getOrCreate(user.getId());
            if (!canSendProactively(settings, current.toLocalTime())) {
                continue;
            }
            boolean sentScheduledMessage = false;
            if (isDue(settings.getMorningEnabled(), settings.getMorningTime(), current)
                    && !hasNotificationToday(settings.getUserId(), DAILY_TASK_TITLE, current.toLocalDate())) {
                sent.add(notificationService.sendDailyTaskReminder(settings.getUserId(), settings.getFeishuChatId()));
                sentScheduledMessage = true;
            }
            if (isDue(settings.getReviewEnabled(), settings.getReviewTime(), current)
                    && !hasNotificationToday(settings.getUserId(), DAILY_REVIEW_TITLE, current.toLocalDate())) {
                sent.add(notificationService.sendDailyReviewReminder(settings.getUserId(), settings.getFeishuChatId()));
                sentScheduledMessage = true;
            }
            if (isWeeklyDue(settings, current)
                    && !hasNotificationToday(settings.getUserId(), WEEKLY_REVIEW_TITLE, current.toLocalDate())) {
                sent.add(notificationService.sendWeeklyReviewReminder(settings.getUserId(), settings.getFeishuChatId()));
                sentScheduledMessage = true;
            }
            if (isAiBriefingDue(settings, current)
                    && !hasNotificationToday(settings.getUserId(), AI_BRIEFING_TITLE, current.toLocalDate())) {
                sent.add(aiBriefingService.sendDailyBriefing(settings.getUserId(), settings));
                sentScheduledMessage = true;
            }
            if (!sentScheduledMessage
                    && isPeriodicNudgeDue(settings)
                    && !hasRecentNotification(settings.getUserId(), PERIODIC_NUDGE_TITLE,
                    current.minusHours(settings.getPeriodicNudgeIntervalHours()))) {
                sent.add(notificationService.sendPeriodicPlanningNudge(settings.getUserId(), settings.getFeishuChatId()));
            }
        }
        return sent;
    }

    private AssistantSettings getOrCreate(Long userId) {
        AssistantSettings existing = getOne(new LambdaQueryWrapper<AssistantSettings>()
                .eq(AssistantSettings::getUserId, userId));
        if (existing != null) {
            fillDefaults(existing);
            return existing;
        }

        AssistantSettings settings = new AssistantSettings();
        settings.setUserId(userId);
        fillDefaults(settings);
        try {
            save(settings);
            return settings;
        } catch (DataIntegrityViolationException ex) {
            AssistantSettings concurrent = getOne(new LambdaQueryWrapper<AssistantSettings>()
                    .eq(AssistantSettings::getUserId, userId));
            if (concurrent != null) {
                fillDefaults(concurrent);
                return concurrent;
            }
            throw ex;
        }
    }

    private void fillDefaults(AssistantSettings settings) {
        if (settings.getProactiveEnabled() == null) {
            settings.setProactiveEnabled(true);
        }
        if (settings.getFeishuEnabled() == null) {
            settings.setFeishuEnabled(true);
        }
        if (settings.getMorningEnabled() == null) {
            settings.setMorningEnabled(true);
        }
        if (settings.getMorningTime() == null) {
            settings.setMorningTime(LocalTime.of(8, 0));
        }
        if (settings.getReviewEnabled() == null) {
            settings.setReviewEnabled(true);
        }
        if (settings.getReviewTime() == null) {
            settings.setReviewTime(LocalTime.of(22, 30));
        }
        if (settings.getWeeklyEnabled() == null) {
            settings.setWeeklyEnabled(true);
        }
        if (settings.getWeeklyDay() == null) {
            settings.setWeeklyDay(7);
        }
        if (settings.getWeeklyTime() == null) {
            settings.setWeeklyTime(LocalTime.of(21, 0));
        }
        if (settings.getPeriodicNudgeEnabled() == null) {
            settings.setPeriodicNudgeEnabled(false);
        }
        if (settings.getPeriodicNudgeIntervalHours() == null) {
            settings.setPeriodicNudgeIntervalHours(3);
        }
        if (settings.getPeriodicNudgeIntervalHours() < 1) {
            settings.setPeriodicNudgeIntervalHours(1);
        }
        if (settings.getPeriodicNudgeIntervalHours() > 24) {
            settings.setPeriodicNudgeIntervalHours(24);
        }
        if (settings.getAiBriefingEnabled() == null) {
            settings.setAiBriefingEnabled(false);
        }
        if (settings.getAiBriefingTime() == null) {
            settings.setAiBriefingTime(LocalTime.of(9, 30));
        }
        if (!StringUtils.hasText(settings.getAiBriefingSourceName())) {
            settings.setAiBriefingSourceName("橘鸦Juya");
        }
        if (settings.getAdviceDays() == null) {
            settings.setAdviceDays(2);
        }
        if (settings.getQuietEnabled() == null) {
            settings.setQuietEnabled(false);
        }
        if (settings.getQuietStartTime() == null) {
            settings.setQuietStartTime(LocalTime.of(23, 30));
        }
        if (settings.getQuietEndTime() == null) {
            settings.setQuietEndTime(LocalTime.of(7, 30));
        }
    }

    private void applyUpdates(AssistantSettings settings, AssistantSettingsUpdateRequest request) {
        if (request.getProactiveEnabled() != null) {
            settings.setProactiveEnabled(request.getProactiveEnabled());
        }
        if (request.getFeishuEnabled() != null) {
            settings.setFeishuEnabled(request.getFeishuEnabled());
        }
        if (request.getFeishuChatId() != null) {
            settings.setFeishuChatId(StringUtils.hasText(request.getFeishuChatId())
                    ? request.getFeishuChatId().trim()
                    : null);
        }
        if (request.getMorningEnabled() != null) {
            settings.setMorningEnabled(request.getMorningEnabled());
        }
        if (request.getMorningTime() != null) {
            settings.setMorningTime(request.getMorningTime());
        }
        if (request.getReviewEnabled() != null) {
            settings.setReviewEnabled(request.getReviewEnabled());
        }
        if (request.getReviewTime() != null) {
            settings.setReviewTime(request.getReviewTime());
        }
        if (request.getWeeklyEnabled() != null) {
            settings.setWeeklyEnabled(request.getWeeklyEnabled());
        }
        if (request.getWeeklyDay() != null) {
            settings.setWeeklyDay(request.getWeeklyDay());
        }
        if (request.getWeeklyTime() != null) {
            settings.setWeeklyTime(request.getWeeklyTime());
        }
        if (request.getPeriodicNudgeEnabled() != null) {
            settings.setPeriodicNudgeEnabled(request.getPeriodicNudgeEnabled());
        }
        if (request.getPeriodicNudgeIntervalHours() != null) {
            settings.setPeriodicNudgeIntervalHours(request.getPeriodicNudgeIntervalHours());
        }
        if (request.getAiBriefingEnabled() != null) {
            settings.setAiBriefingEnabled(request.getAiBriefingEnabled());
        }
        if (request.getAiBriefingTime() != null) {
            settings.setAiBriefingTime(request.getAiBriefingTime());
        }
        if (request.getAiBriefingSourceName() != null) {
            settings.setAiBriefingSourceName(StringUtils.hasText(request.getAiBriefingSourceName())
                    ? request.getAiBriefingSourceName().trim()
                    : "橘鸦Juya");
        }
        if (request.getAiBriefingSourceUrl() != null) {
            settings.setAiBriefingSourceUrl(StringUtils.hasText(request.getAiBriefingSourceUrl())
                    ? request.getAiBriefingSourceUrl().trim()
                    : null);
        }
        if (request.getAdviceDays() != null) {
            settings.setAdviceDays(request.getAdviceDays());
        }
        if (request.getQuietEnabled() != null) {
            settings.setQuietEnabled(request.getQuietEnabled());
        }
        if (request.getQuietStartTime() != null) {
            settings.setQuietStartTime(request.getQuietStartTime());
        }
        if (request.getQuietEndTime() != null) {
            settings.setQuietEndTime(request.getQuietEndTime());
        }
        fillDefaults(settings);
    }

    private boolean canSendProactively(AssistantSettings settings, LocalTime now) {
        return reminderProperties.isEnabled()
                && Boolean.TRUE.equals(settings.getProactiveEnabled())
                && Boolean.TRUE.equals(settings.getFeishuEnabled())
                && !isInQuietHours(settings, now);
    }

    private boolean isDue(Boolean enabled, LocalTime scheduledTime, LocalDateTime now) {
        if (!Boolean.TRUE.equals(enabled) || scheduledTime == null) {
            return false;
        }
        return scheduledTime.getHour() == now.getHour()
                && scheduledTime.getMinute() == now.getMinute();
    }

    private boolean isWeeklyDue(AssistantSettings settings, LocalDateTime now) {
        return Boolean.TRUE.equals(settings.getWeeklyEnabled())
                && settings.getWeeklyDay() != null
                && settings.getWeeklyDay() == now.getDayOfWeek().getValue()
                && isDue(true, settings.getWeeklyTime(), now);
    }

    private boolean isAiBriefingDue(AssistantSettings settings, LocalDateTime now) {
        return Boolean.TRUE.equals(settings.getAiBriefingEnabled())
                && StringUtils.hasText(settings.getAiBriefingSourceUrl())
                && isDue(true, settings.getAiBriefingTime(), now);
    }

    private boolean isPeriodicNudgeDue(AssistantSettings settings) {
        return Boolean.TRUE.equals(settings.getPeriodicNudgeEnabled())
                && settings.getPeriodicNudgeIntervalHours() != null
                && settings.getPeriodicNudgeIntervalHours() > 0;
    }

    private boolean isInQuietHours(AssistantSettings settings, LocalTime now) {
        if (!Boolean.TRUE.equals(settings.getQuietEnabled())
                || settings.getQuietStartTime() == null
                || settings.getQuietEndTime() == null) {
            return false;
        }
        LocalTime start = settings.getQuietStartTime();
        LocalTime end = settings.getQuietEndTime();
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private boolean hasNotificationToday(Long userId, String titlePrefix, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .likeRight(Notification::getTitle, titlePrefix)
                .ge(Notification::getNotifyTime, start)
                .lt(Notification::getNotifyTime, end));
        return count != null && count > 0;
    }

    private boolean hasRecentNotification(Long userId, String titlePrefix, LocalDateTime since) {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .likeRight(Notification::getTitle, titlePrefix)
                .ge(Notification::getNotifyTime, since));
        return count != null && count > 0;
    }

    private AssistantSettingsVO toVO(AssistantSettings settings) {
        AssistantSettingsVO vo = new AssistantSettingsVO();
        BeanUtils.copyProperties(settings, vo);
        boolean defaultChatConfigured = StringUtils.hasText(feishuProperties.getDefaultChatId());
        boolean customChatConfigured = StringUtils.hasText(settings.getFeishuChatId());
        vo.setGlobalReminderEnabled(reminderProperties.isEnabled());
        vo.setFeishuAppConfigured(feishuOpenApiClient.isConfigured());
        vo.setFeishuDefaultChatConfigured(defaultChatConfigured);
        vo.setEffectiveFeishuChatConfigured(customChatConfigured || defaultChatConfigured);
        vo.setFeishuLongConnectionEnabled(feishuProperties.isLongConnectionEnabled());
        vo.setDifyChatConfigured(difyClient.isConfigured());
        vo.setDifyWorkflowConfigured(difyClient.isWorkflowConfigured());
        vo.setAiBriefingSourceConfigured(StringUtils.hasText(settings.getAiBriefingSourceUrl()));
        return vo;
    }
}

package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.settings.AssistantSettingsUpdateRequest;
import com.example.goalbot.entity.AssistantSettings;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.vo.AssistantSettingsVO;

import java.time.LocalDateTime;
import java.util.List;

public interface AssistantSettingsService extends IService<AssistantSettings> {

    AssistantSettingsVO getSettings(Long userId);

    AssistantSettingsVO updateSettings(Long userId, AssistantSettingsUpdateRequest request);

    boolean bindFeishuChatIfAbsent(Long userId, String chatId);

    Notification sendTestMessage(Long userId);

    Notification sendNow(Long userId, String type);

    List<Notification> sendDueProactiveMessages(LocalDateTime now);
}

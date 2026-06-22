package com.example.goalbot.integration.feishu;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.goalbot.entity.User;
import com.example.goalbot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class FeishuUserResolver {

    private final UserMapper userMapper;

    public Long resolveUserId(String userId, String openId, String unionId) {
        for (String candidate : Arrays.asList(userId, openId, unionId)) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            User user = userMapper.selectOne(new QueryWrapper<User>()
                    .eq("feishu_user_id", candidate)
                    .eq("status", 1)
                    .last("limit 1"));
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }
}

package com.example.goalbot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.common.PasswordHasher;
import com.example.goalbot.entity.User;
import com.example.goalbot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final AuthProperties authProperties;

    @Override
    public void run(ApplicationArguments args) {
        String username = StringUtils.hasText(authProperties.getBootstrapAdminUsername())
                ? authProperties.getBootstrapAdminUsername().trim()
                : "local_user";
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        String bootstrapPassword = authProperties.getBootstrapAdminPassword();

        if (user == null) {
            if (!StringUtils.hasText(bootstrapPassword)) {
                log.warn("GoalBot has no bootstrap admin '{}'. Set GOALBOT_BOOTSTRAP_ADMIN_PASSWORD before first login.", username);
                return;
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setPassword(passwordHasher.hash(bootstrapPassword));
            admin.setNickname("GoalBot 管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("GoalBot bootstrap administrator '{}' was created.", username);
            return;
        }

        if (!passwordHasher.isEncoded(user.getPassword())) {
            if (!StringUtils.hasText(bootstrapPassword)) {
                log.warn("Bootstrap administrator '{}' still has no usable password. Set GOALBOT_BOOTSTRAP_ADMIN_PASSWORD.", username);
                return;
            }
            user.setPassword(passwordHasher.hash(bootstrapPassword));
            user.setRole("ADMIN");
            user.setStatus(1);
            userMapper.updateById(user);
            log.info("GoalBot bootstrap administrator '{}' password was initialized.", username);
        }
    }
}

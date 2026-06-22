package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.goalbot.common.AuthenticatedUser;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.common.PasswordHasher;
import com.example.goalbot.config.AuthProperties;
import com.example.goalbot.dto.auth.LoginRequest;
import com.example.goalbot.dto.auth.PasswordChangeRequest;
import com.example.goalbot.entity.AuthSession;
import com.example.goalbot.entity.User;
import com.example.goalbot.mapper.AuthSessionMapper;
import com.example.goalbot.mapper.UserMapper;
import com.example.goalbot.service.AuthService;
import com.example.goalbot.vo.LoginVO;
import com.example.goalbot.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int TOKEN_BYTES = 32;

    private final UserMapper userMapper;
    private final AuthSessionMapper authSessionMapper;
    private final PasswordHasher passwordHasher;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (user == null || !passwordHasher.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BusinessException.forbidden("该用户已被停用");
        }

        LocalDateTime now = LocalDateTime.now();
        authSessionMapper.delete(new LambdaQueryWrapper<AuthSession>()
                .lt(AuthSession::getExpiresAt, now));

        String token = generateToken();
        AuthSession session = new AuthSession();
        session.setUserId(user.getId());
        session.setTokenHash(hashToken(token));
        session.setExpiresAt(now.plusDays(Math.max(1, authProperties.getSessionDays())));
        session.setLastAccessedAt(now);
        authSessionMapper.insert(session);

        user.setLastLoginAt(now);
        userMapper.updateById(user);
        return new LoginVO(token, session.getExpiresAt(), toVO(user));
    }

    @Override
    public AuthenticatedUser authenticate(String token) {
        if (!StringUtils.hasText(token)) {
            throw BusinessException.unauthorized("请先登录");
        }
        AuthSession session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getTokenHash, hashToken(token))
                .gt(AuthSession::getExpiresAt, LocalDateTime.now())
                .last("limit 1"));
        if (session == null) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        User user = userMapper.selectById(session.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw BusinessException.unauthorized("用户不存在或已被停用");
        }
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        authSessionMapper.delete(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getTokenHash, hashToken(token)));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!passwordHasher.matches(request.getCurrentPassword(), user.getPassword())) {
            throw BusinessException.badRequest("当前密码错误");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw BusinessException.badRequest("新密码不能与当前密码相同");
        }
        user.setPassword(passwordHasher.hash(request.getNewPassword()));
        userMapper.updateById(user);
        authSessionMapper.delete(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getUserId, userId));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Token hashing is unavailable", ex);
        }
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}

package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.common.PasswordHasher;
import com.example.goalbot.dto.user.PasswordResetRequest;
import com.example.goalbot.dto.user.UserCreateRequest;
import com.example.goalbot.dto.user.UserUpdateRequest;
import com.example.goalbot.entity.AuthSession;
import com.example.goalbot.entity.User;
import com.example.goalbot.mapper.AuthSessionMapper;
import com.example.goalbot.mapper.UserMapper;
import com.example.goalbot.service.UserManagementService;
import com.example.goalbot.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserMapper userMapper;
    private final AuthSessionMapper authSessionMapper;
    private final PasswordHasher passwordHasher;

    @Override
    public List<UserVO> listUsers(String keyword, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), query -> query
                        .like(User::getUsername, keyword.trim())
                        .or()
                        .like(User::getNickname, keyword.trim()))
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getStatus)
                .orderByAsc(User::getId);
        return userMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public UserVO createUser(UserCreateRequest request) {
        String username = request.getUsername().trim();
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
        String feishuUserId = normalizeNullable(request.getFeishuUserId());
        ensureFeishuIdAvailable(feishuUserId, null);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordHasher.hash(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setFeishuUserId(feishuUserId);
        user.setRole(normalizeRole(request.getRole()));
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict("用户名或飞书用户 ID 已存在");
        }
        return toVO(userMapper.selectById(user.getId()));
    }

    @Override
    @Transactional
    public UserVO updateUser(Long operatorId, Long userId, UserUpdateRequest request) {
        User user = requireUser(userId);
        String nextRole = request.getRole() == null ? user.getRole() : normalizeRole(request.getRole());
        Integer nextStatus = request.getStatus() == null ? user.getStatus() : request.getStatus();
        if (operatorId.equals(userId) && (!"ADMIN".equals(nextRole) || !Integer.valueOf(1).equals(nextStatus))) {
            throw BusinessException.badRequest("不能停用自己或移除自己的管理员权限");
        }

        if (request.getNickname() != null) {
            user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : user.getUsername());
        }
        if (request.getFeishuUserId() != null) {
            String feishuUserId = normalizeNullable(request.getFeishuUserId());
            ensureFeishuIdAvailable(feishuUserId, userId);
            user.setFeishuUserId(feishuUserId);
        }
        user.setRole(nextRole);
        user.setStatus(nextStatus);
        try {
            userMapper.updateById(user);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict("飞书用户 ID 已绑定给其他用户");
        }
        if (!Integer.valueOf(1).equals(nextStatus)) {
            authSessionMapper.delete(new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getUserId, userId));
        }
        return toVO(userMapper.selectById(userId));
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, PasswordResetRequest request) {
        User user = requireUser(userId);
        user.setPassword(passwordHasher.hash(request.getNewPassword()));
        userMapper.updateById(user);
        authSessionMapper.delete(new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getUserId, userId));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private void ensureFeishuIdAvailable(String feishuUserId, Long excludedUserId) {
        if (!StringUtils.hasText(feishuUserId)) {
            return;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getFeishuUserId, feishuUserId)
                .ne(excludedUserId != null, User::getId, excludedUserId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("飞书用户 ID 已绑定给其他用户");
        }
    }

    private String normalizeRole(String role) {
        String normalized = StringUtils.hasText(role) ? role.trim().toUpperCase(Locale.ROOT) : "USER";
        if (!"ADMIN".equals(normalized) && !"USER".equals(normalized)) {
            throw BusinessException.badRequest("角色只支持 ADMIN 或 USER");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}

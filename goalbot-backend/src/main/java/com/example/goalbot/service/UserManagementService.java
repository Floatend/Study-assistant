package com.example.goalbot.service;

import com.example.goalbot.dto.user.PasswordResetRequest;
import com.example.goalbot.dto.user.UserCreateRequest;
import com.example.goalbot.dto.user.UserUpdateRequest;
import com.example.goalbot.vo.UserVO;

import java.util.List;

public interface UserManagementService {

    List<UserVO> listUsers(String keyword, Integer status);

    UserVO createUser(UserCreateRequest request);

    UserVO updateUser(Long operatorId, Long userId, UserUpdateRequest request);

    void resetPassword(Long userId, PasswordResetRequest request);
}

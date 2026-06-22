package com.example.goalbot.service;

import com.example.goalbot.common.AuthenticatedUser;
import com.example.goalbot.dto.auth.LoginRequest;
import com.example.goalbot.dto.auth.PasswordChangeRequest;
import com.example.goalbot.vo.LoginVO;
import com.example.goalbot.vo.UserVO;

public interface AuthService {

    LoginVO login(LoginRequest request);

    AuthenticatedUser authenticate(String token);

    UserVO getCurrentUser(Long userId);

    void logout(String token);

    void changePassword(Long userId, PasswordChangeRequest request);
}

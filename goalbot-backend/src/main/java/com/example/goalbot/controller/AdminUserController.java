package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.user.PasswordResetRequest;
import com.example.goalbot.dto.user.UserCreateRequest;
import com.example.goalbot.dto.user.UserUpdateRequest;
import com.example.goalbot.service.UserManagementService;
import com.example.goalbot.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController extends BaseController {

    private final UserManagementService userManagementService;

    @GetMapping
    public Result<List<UserVO>> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        requireAdmin();
        return Result.success(userManagementService.listUsers(keyword, status));
    }

    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        requireAdmin();
        return Result.success(userManagementService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateRequest request) {
        requireAdmin();
        return Result.success(userManagementService.updateUser(currentUser().id(), id, request));
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody PasswordResetRequest request) {
        requireAdmin();
        userManagementService.resetPassword(id, request);
        return Result.success();
    }
}

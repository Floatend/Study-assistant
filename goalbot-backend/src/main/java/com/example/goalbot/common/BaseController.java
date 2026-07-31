package com.example.goalbot.common;

public abstract class BaseController {

    protected Long currentUserId(Long ignoredHeaderUserId) {
        return AuthContext.requireUser().id();
    }

    protected AuthenticatedUser currentUser() {
        return AuthContext.requireUser();
    }

    protected void requireAdmin() {
        if (!currentUser().isAdmin()) {
            throw BusinessException.forbidden("需要站长管理员权限");
        }
    }
}

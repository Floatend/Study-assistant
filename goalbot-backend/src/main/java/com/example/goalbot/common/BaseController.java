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
            throw BusinessException.forbidden("Administrator permission is required");
        }
    }
}

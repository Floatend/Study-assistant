package com.example.goalbot.common;

public final class AuthContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static AuthenticatedUser requireUser() {
        AuthenticatedUser user = CURRENT.get();
        if (user == null) {
            throw BusinessException.unauthorized("Please sign in first");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}

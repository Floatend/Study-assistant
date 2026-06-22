package com.example.goalbot.common;

public record AuthenticatedUser(Long id, String username, String nickname, String role) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}

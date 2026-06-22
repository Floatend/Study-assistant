package com.example.goalbot.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "Conflict"),
    INTERNAL_ERROR(500, "Internal server error"),
    EXTERNAL_SERVICE_ERROR(600, "External service error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

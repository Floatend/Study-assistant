package com.example.goalbot.integration.dify;

public class DifyException extends RuntimeException {

    public DifyException(String message) {
        super(message);
    }

    public DifyException(String message, Throwable cause) {
        super(message, cause);
    }
}

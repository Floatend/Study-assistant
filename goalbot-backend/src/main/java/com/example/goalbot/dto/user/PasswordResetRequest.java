package com.example.goalbot.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}

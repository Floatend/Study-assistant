package com.example.goalbot.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "只能包含字母、数字、点、下划线和短横线")
    private String username;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @Size(max = 64)
    private String nickname;

    @Size(max = 128)
    private String feishuUserId;

    private String role = "USER";
}

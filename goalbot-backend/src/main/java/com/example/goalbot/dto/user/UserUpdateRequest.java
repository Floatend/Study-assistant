package com.example.goalbot.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(max = 64)
    private String nickname;

    @Size(max = 128)
    private String feishuUserId;

    private String role;

    @Min(0)
    @Max(1)
    private Integer status;
}

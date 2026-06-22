package com.example.goalbot.dto.feishu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeishuCommandTestRequest {

    @NotBlank
    private String text;
}

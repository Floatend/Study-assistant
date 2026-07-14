package com.example.goalbot.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteCreateRequest {

    @NotBlank
    @Size(max = 160)
    private String title;

    @NotBlank
    private String content;

    @Size(max = 255)
    private String tags;

    private Boolean published;

    private Boolean official;
}

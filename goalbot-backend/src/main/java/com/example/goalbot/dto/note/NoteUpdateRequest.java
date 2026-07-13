package com.example.goalbot.dto.note;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteUpdateRequest {

    @Size(max = 160)
    private String title;

    private String content;

    @Size(max = 255)
    private String tags;

    private Boolean published;
}

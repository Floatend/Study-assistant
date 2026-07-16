package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteVO {

    private Long id;

    private Long userId;

    private String title;

    private String fileName;

    private String summary;

    private String content;

    private String tags;

    private String category;

    private Boolean published;

    private Boolean official;

    private String authorName;

    private Integer wordCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.example.goalbot.vo;

import java.time.LocalDateTime;

public record PublicNoteItemVO(Long id, String title, String category, String tags,
                               String excerpt, Integer wordCount, LocalDateTime updatedAt) {
}

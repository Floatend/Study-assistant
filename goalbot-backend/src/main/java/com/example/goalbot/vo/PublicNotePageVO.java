package com.example.goalbot.vo;

import java.util.List;

public record PublicNotePageVO(List<PublicNoteItemVO> items, long total, int page, int pageSize) {
}

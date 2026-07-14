package com.example.goalbot.controller;

import com.example.goalbot.common.Result;
import com.example.goalbot.service.NoteService;
import com.example.goalbot.vo.NoteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only site content boundary. Only notes explicitly approved for the official site are exposed here.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/notes")
public class PublicNoteController {

    private final NoteService noteService;

    @GetMapping
    public Result<List<NoteVO>> listOfficialNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.success(noteService.listOfficialNotes(keyword, limit));
    }

    @GetMapping("/{id}")
    public Result<NoteVO> getOfficialNote(@PathVariable Long id) {
        return Result.success(noteService.getOfficialNote(id));
    }
}

package com.example.goalbot.controller;

import com.example.goalbot.common.Result;
import com.example.goalbot.service.NoteService;
import com.example.goalbot.service.PublicNoteSearchService;
import com.example.goalbot.vo.PublicNoteItemVO;
import com.example.goalbot.vo.PublicNotePageVO;
import com.example.goalbot.vo.PublicNoteNavigationVO;
import com.example.goalbot.vo.NoteVO;
import com.example.goalbot.vo.NoteCategoryVO;
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
    private final PublicNoteSearchService searchService;

    @GetMapping("/search")
    public Result<PublicNotePageVO> search(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "true") boolean descendants,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return Result.success(searchService.search(keyword, category, descendants, page, pageSize));
    }

    @GetMapping("/{id}/related")
    public Result<List<PublicNoteItemVO>> related(@PathVariable Long id) {
        return Result.success(searchService.related(id));
    }

    @GetMapping("/{id}/navigation")
    public Result<PublicNoteNavigationVO> navigation(@PathVariable Long id,
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "true") boolean descendants) {
        return Result.success(searchService.navigation(id, keyword, category, descendants));
    }

    @GetMapping
    public Result<List<NoteVO>> listOfficialNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer limit) {
        return Result.success(noteService.listOfficialNotes(keyword, category, limit));
    }

    @GetMapping("/categories")
    public Result<List<NoteCategoryVO>> listOfficialCategories() {
        return Result.success(noteService.listOfficialCategories());
    }

    @GetMapping("/{id}")
    public Result<NoteVO> getOfficialNote(@PathVariable Long id) {
        return Result.success(noteService.getOfficialNote(id));
    }
}

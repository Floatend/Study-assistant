package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.note.NoteCreateRequest;
import com.example.goalbot.dto.note.NoteUpdateRequest;
import com.example.goalbot.service.NoteService;
import com.example.goalbot.vo.NoteVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notes")
public class NoteController extends BaseController {

    private final NoteService noteService;

    @GetMapping
    public Result<List<NoteVO>> listNotes(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.success(noteService.listNotes(currentUserId(headerUserId), keyword, limit));
    }

    @GetMapping("/{id}")
    public Result<NoteVO> getNote(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id) {
        return Result.success(noteService.getNote(currentUserId(headerUserId), id));
    }

    @PostMapping
    public Result<NoteVO> createNote(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody NoteCreateRequest request) {
        return Result.success(noteService.createNote(currentUserId(headerUserId), request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<NoteVO> uploadNote(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tags) {
        return Result.success(noteService.uploadNote(currentUserId(headerUserId), file, title, tags));
    }

    @PutMapping("/{id}")
    public Result<NoteVO> updateNote(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id,
            @Valid @RequestBody NoteUpdateRequest request) {
        return Result.success(noteService.updateNote(currentUserId(headerUserId), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id) {
        noteService.deleteNote(currentUserId(headerUserId), id);
        return Result.success();
    }
}

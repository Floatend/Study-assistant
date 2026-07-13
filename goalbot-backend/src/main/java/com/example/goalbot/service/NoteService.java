package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.note.NoteCreateRequest;
import com.example.goalbot.dto.note.NoteUpdateRequest;
import com.example.goalbot.entity.Note;
import com.example.goalbot.vo.NoteVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NoteService extends IService<Note> {

    List<NoteVO> listNotes(Long userId, String keyword, Integer limit);

    List<NoteVO> listPublishedNotes(String keyword, Integer limit);

    NoteVO getNote(Long userId, Long id);

    NoteVO getPublishedNote(Long id);

    NoteVO createNote(Long userId, NoteCreateRequest request);

    NoteVO uploadNote(Long userId, MultipartFile file, String title, String tags);

    NoteVO updateNote(Long userId, Long id, NoteUpdateRequest request);

    void deleteNote(Long userId, Long id);
}

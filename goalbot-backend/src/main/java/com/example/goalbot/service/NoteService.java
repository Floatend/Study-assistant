package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.note.NoteCreateRequest;
import com.example.goalbot.dto.note.NoteUpdateRequest;
import com.example.goalbot.entity.Note;
import com.example.goalbot.vo.NoteVO;
import com.example.goalbot.vo.NoteCategoryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NoteService extends IService<Note> {

    List<NoteVO> listNotes(Long userId, String keyword, String category, Boolean published, Integer limit);

    List<NoteVO> listOfficialNotes(String keyword, String category, Integer limit);

    List<NoteCategoryVO> listCategories(Long userId);

    List<NoteCategoryVO> listOfficialCategories();

    NoteVO getNote(Long userId, Long id);

    NoteVO getOfficialNote(Long id);

    NoteVO createNote(Long userId, NoteCreateRequest request);

    NoteVO uploadNote(Long userId, MultipartFile file, String title, String tags, String category);

    NoteVO updateNote(Long userId, Long id, NoteUpdateRequest request);

    void deleteNote(Long userId, Long id);
}

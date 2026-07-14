package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.dto.note.NoteCreateRequest;
import com.example.goalbot.dto.note.NoteUpdateRequest;
import com.example.goalbot.entity.Note;
import com.example.goalbot.entity.User;
import com.example.goalbot.mapper.NoteMapper;
import com.example.goalbot.mapper.UserMapper;
import com.example.goalbot.service.NoteService;
import com.example.goalbot.vo.NoteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 50;
    private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;

    private final UserMapper userMapper;

    @Override
    public List<NoteVO> listNotes(Long userId, String keyword, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Note::getTitle, keyword)
                        .or()
                        .like(Note::getSummary, keyword)
                        .or()
                        .like(Note::getTags, keyword))
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getCreatedAt)
                .last("LIMIT " + normalizedLimit);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<NoteVO> listOfficialNotes(String keyword, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        List<Note> notes = list(new LambdaQueryWrapper<Note>()
                .eq(Note::getPublished, true)
                .eq(Note::getOfficial, true)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Note::getTitle, keyword)
                        .or()
                        .like(Note::getSummary, keyword)
                        .or()
                        .like(Note::getTags, keyword))
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getCreatedAt)
                .last("LIMIT " + normalizedLimit));
        if (notes.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = notes.stream().map(Note::getUserId).collect(Collectors.toSet());
        Map<Long, String> authorNames = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::displayName, (first, ignored) -> first));
        return notes.stream()
                .map(note -> toPublicVO(note, authorNames.get(note.getUserId())))
                .toList();
    }

    @Override
    public NoteVO getNote(Long userId, Long id) {
        return toVO(getOwnedNote(userId, id));
    }

    @Override
    public NoteVO getOfficialNote(Long id) {
        Note note = getOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getPublished, true)
                .eq(Note::getOfficial, true));
        if (note == null) {
            throw BusinessException.notFound("Official note not found");
        }
        User author = userMapper.selectById(note.getUserId());
        return toPublicVO(note, author == null ? null : displayName(author));
    }

    @Override
    @Transactional
    public NoteVO createNote(Long userId, NoteCreateRequest request) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(cleanTitle(request.getTitle()));
        note.setContent(request.getContent().trim());
        note.setTags(cleanNullable(request.getTags()));
        boolean official = Boolean.TRUE.equals(request.getOfficial());
        note.setOfficial(official);
        note.setPublished(official);
        refreshDerivedFields(note);
        save(note);
        return toVO(note);
    }

    @Override
    @Transactional
    public NoteVO uploadNote(Long userId, MultipartFile file, String title, String tags) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Note file is required");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw BusinessException.badRequest("Note file cannot exceed 2 MB");
        }
        String fileName = cleanNullable(file.getOriginalFilename());
        if (!isSupportedFile(fileName)) {
            throw BusinessException.badRequest("Only .md, .markdown, and .txt files are supported");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw BusinessException.badRequest("Failed to read note file");
        }
        if (!StringUtils.hasText(content)) {
            throw BusinessException.badRequest("Note file is empty");
        }

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(cleanTitle(StringUtils.hasText(title) ? title : titleFromFileName(fileName)));
        note.setFileName(fileName);
        note.setContent(content);
        note.setTags(cleanNullable(tags));
        note.setPublished(false);
        note.setOfficial(false);
        refreshDerivedFields(note);
        save(note);
        return toVO(note);
    }

    @Override
    @Transactional
    public NoteVO updateNote(Long userId, Long id, NoteUpdateRequest request) {
        Note note = getOwnedNote(userId, id);
        if (StringUtils.hasText(request.getTitle())) {
            note.setTitle(cleanTitle(request.getTitle()));
        }
        if (request.getContent() != null) {
            if (!StringUtils.hasText(request.getContent())) {
                throw BusinessException.badRequest("Note content cannot be empty");
            }
            note.setContent(request.getContent().trim());
        }
        if (request.getTags() != null) {
            note.setTags(cleanNullable(request.getTags()));
        }
        if (request.getOfficial() != null) {
            note.setOfficial(request.getOfficial());
            note.setPublished(request.getOfficial());
        }
        refreshDerivedFields(note);
        updateById(note);
        return toVO(getById(id));
    }

    @Override
    @Transactional
    public void deleteNote(Long userId, Long id) {
        Note note = getOwnedNote(userId, id);
        removeById(note.getId());
    }

    private Note getOwnedNote(Long userId, Long id) {
        Note note = getOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getUserId, userId));
        if (note == null) {
            throw BusinessException.notFound("Note not found");
        }
        return note;
    }

    private void refreshDerivedFields(Note note) {
        note.setSummary(buildSummary(note.getContent()));
        note.setWordCount(countWords(note.getContent()));
    }

    private String buildSummary(String content) {
        String normalized = content == null ? "" : content
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^>\\s*", "")
                .replaceAll("[`*_>#\\-\\[\\]()]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180) + "...";
    }

    private int countWords(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return content.replaceAll("\\s+", "").length();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private boolean isSupportedFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return true;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt");
    }

    private String titleFromFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "Untitled note";
        }
        String normalized = fileName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
        int dot = baseName.lastIndexOf('.');
        return dot > 0 ? baseName.substring(0, dot) : baseName;
    }

    private String cleanTitle(String title) {
        String cleaned = cleanNullable(title);
        if (!StringUtils.hasText(cleaned)) {
            throw BusinessException.badRequest("Note title is required");
        }
        return cleaned.length() <= 160 ? cleaned : cleaned.substring(0, 160);
    }

    private String cleanNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private NoteVO toVO(Note note) {
        NoteVO vo = new NoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }

    private NoteVO toPublicVO(Note note, String authorName) {
        NoteVO vo = toVO(note);
        vo.setAuthorName(authorName);
        return vo;
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}

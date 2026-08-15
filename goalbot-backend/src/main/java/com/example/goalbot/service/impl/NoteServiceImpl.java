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
import com.example.goalbot.vo.NoteCategoryVO;
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

    private static final int DEFAULT_LIMIT = 24;
    private static final int MAX_LIMIT = 100;
    private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;
    private static final String UNCATEGORIZED = "未分类";

    private final UserMapper userMapper;

    @Override
    public List<NoteVO> listNotes(Long userId, String keyword, String category, Boolean published, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(published != null, Note::getPublished, published)
                .and(StringUtils.hasText(category), query -> applyCategoryFilter(query, category))
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Note::getTitle, keyword)
                        .or()
                        .like(Note::getSummary, keyword)
                        .or()
                        .like(Note::getTags, keyword)
                        .or()
                        .like(Note::getCategory, keyword))
                .orderByDesc(Note::getUpdatedAt)
                .orderByDesc(Note::getCreatedAt)
                .last("LIMIT " + normalizedLimit);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<NoteVO> listOfficialNotes(String keyword, String category, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        List<Note> notes = list(new LambdaQueryWrapper<Note>()
                .eq(Note::getPublished, true)
                .eq(Note::getOfficial, true)
                .and(StringUtils.hasText(category), query -> applyCategoryFilter(query, category))
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Note::getTitle, keyword)
                        .or()
                        .like(Note::getSummary, keyword)
                        .or()
                        .like(Note::getTags, keyword)
                        .or()
                        .like(Note::getCategory, keyword))
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
    public List<NoteCategoryVO> listCategories(Long userId) {
        return toCategoryVOs(list(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .select(Note::getCategory)));
    }

    @Override
    public List<NoteCategoryVO> listOfficialCategories() {
        return toCategoryVOs(list(new LambdaQueryWrapper<Note>()
                .eq(Note::getPublished, true)
                .eq(Note::getOfficial, true)
                .select(Note::getCategory)));
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
        note.setCategory(cleanCategory(request.getCategory()));
        boolean official = Boolean.TRUE.equals(request.getOfficial());
        note.setOfficial(official);
        note.setPublished(official || Boolean.TRUE.equals(request.getPublished()));
        refreshDerivedFields(note);
        save(note);
        return toVO(note);
    }

    @Override
    @Transactional
    public NoteVO uploadNote(Long userId, MultipartFile file, String title, String tags, String category) {
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
        note.setCategory(cleanCategory(category));
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
        if (request.getCategory() != null) {
            note.setCategory(cleanCategory(request.getCategory()));
        }
        if (request.getPublished() != null) {
            note.setPublished(request.getPublished());
            if (!request.getPublished()) {
                note.setOfficial(false);
            }
        }
        if (request.getOfficial() != null) {
            note.setOfficial(request.getOfficial());
            if (request.getOfficial()) {
                note.setPublished(true);
            }
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
        if (!StringUtils.hasText(content)) {
            return "";
        }

        String[] lines = content.replace("\r\n", "\n").split("\n");
        StringBuilder excerpt = new StringBuilder();
        boolean inCodeFence = false;
        boolean frontmatterClosed = !isFrontmatterDelimiter(lines[0].trim());

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!frontmatterClosed) {
                if (isFrontmatterDelimiter(line)) {
                    frontmatterClosed = true;
                }
                continue;
            }
            if (inCodeFence) {
                if (line.startsWith("```") || line.startsWith("~~~")) {
                    inCodeFence = false;
                }
                continue;
            }
            if (line.startsWith("```") || line.startsWith("~~~")) {
                inCodeFence = true;
                continue;
            }
            if (line.startsWith("#") || line.matches("^---+\\s*$") || line.startsWith("<!--")) {
                continue;
            }

            boolean isBlockquote = line.startsWith(">");
            String text = line.replaceFirst("^>\\s*", "").trim();
            if (!StringUtils.hasText(text) || text.startsWith("[!") || text.matches("^!\\w+.*") || text.startsWith("|")) {
                continue;
            }
            if (text.contains("$") || text.contains("\\(") || text.contains("\\)") || text.contains("\\[")) {
                continue;
            }
            if (text.contains("|")) {
                text = text.replaceAll("\\s*\\|\\s*", "、");
            }

            String plain = stripInlineMarkdown(text);
            if (!StringUtils.hasText(plain)) {
                continue;
            }

            if (isBlockquote && plain.length() <= 160) {
                return plain;
            }

            if (excerpt.length() > 0) {
                excerpt.append(' ');
            }
            excerpt.append(plain);
            if (excerpt.length() >= 180 || line.isEmpty()) {
                break;
            }
        }

        String result = excerpt.toString().replaceAll("\\s+", " ").trim();
        if (StringUtils.hasText(result)) {
            return truncateSummary(result);
        }

        for (String rawLine : lines) {
            String text = rawLine.trim().replaceFirst("^#{1,6}\\s*", "").replaceFirst("^>\\s*", "").trim();
            if (StringUtils.hasText(text) && !text.contains("$") && !text.contains("|") && !text.contains("\\")) {
                return truncateSummary(stripInlineMarkdown(text));
            }
        }
        return "";
    }

    private boolean isFrontmatterDelimiter(String line) {
        return line.matches("^(---|\\+\\+\\+)\\s*$");
    }

    private String stripInlineMarkdown(String text) {
        return text
                .replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("__([^_]+)__", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("_([^_]+)_", "$1")
                .replaceAll("~~([^~]+)~~", "$1")
                .replaceAll("==([^=]+)==", "$1")
                .replaceAll("^[\\-+*]\\s+", "")
                .replaceAll("^\\d+\\.\\s+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncateSummary(String value) {
        if (value.length() <= 180) {
            return value;
        }
        return value.substring(0, 180).trim() + "...";
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

    private void applyCategoryFilter(LambdaQueryWrapper<Note> wrapper, String category) {
        String normalized = cleanCategory(category);
        if (UNCATEGORIZED.equals(normalized)) {
            wrapper.and(query -> query.isNull(Note::getCategory).or().eq(Note::getCategory, ""));
            return;
        }
        wrapper.eq(Note::getCategory, normalized);
    }

    private List<NoteCategoryVO> toCategoryVOs(List<Note> notes) {
        Map<String, Long> counts = notes.stream()
                .map(Note::getCategory)
                .map(this::displayCategory)
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new NoteCategoryVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String displayCategory(String category) {
        return StringUtils.hasText(category) ? category : UNCATEGORIZED;
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

    private String cleanCategory(String value) {
        String cleaned = cleanNullable(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        return cleaned.length() <= 64 ? cleaned : cleaned.substring(0, 64);
    }

    private NoteVO toVO(Note note) {
        NoteVO vo = new NoteVO();
        BeanUtils.copyProperties(note, vo);
        vo.setSummary(buildSummary(note.getContent()));
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

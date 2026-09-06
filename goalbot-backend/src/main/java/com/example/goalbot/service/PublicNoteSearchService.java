package com.example.goalbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.common.NoteExcerpt;
import com.example.goalbot.entity.Note;
import com.example.goalbot.mapper.NoteMapper;
import com.example.goalbot.vo.PublicNoteItemVO;
import com.example.goalbot.vo.PublicNoteNavigationVO;
import com.example.goalbot.vo.PublicNotePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicNoteSearchService {
    private final NoteMapper noteMapper;

    public PublicNotePageVO search(String keyword, String category, boolean descendants, int page, int pageSize) {
        validate(keyword, category, page, pageSize);
        LambdaQueryWrapper<Note> query = searchQuery(keyword, category, descendants);
        long total = noteMapper.selectCount(query);
        int actualPage = (int) Math.min(page, Math.max(1, (total + pageSize - 1) / pageSize));
        long offset = (long) (actualPage - 1) * pageSize;
        List<PublicNoteItemVO> items = total == 0 ? List.of() : noteMapper.selectList(newest(query)
                .last("LIMIT " + pageSize + " OFFSET " + offset)).stream().map(note -> item(note, keyword)).toList();
        return new PublicNotePageVO(items, total, actualPage, pageSize);
    }

    public List<PublicNoteItemVO> related(Long id) {
        Note anchor = requirePublic(id);
        LambdaQueryWrapper<Note> query = publicQuery().ne(Note::getId, id);
        exactCategory(query, anchor.getCategory());
        return noteMapper.selectList(newest(query).last("LIMIT 4")).stream().map(note -> item(note, "")).toList();
    }

    public PublicNoteNavigationVO navigation(Long id, String keyword, String category, boolean descendants) {
        validate(keyword, category, 1, 12);
        Note anchor = requirePublic(id);
        if (noteMapper.selectCount(searchQuery(keyword, category, descendants).eq(Note::getId, id)) == 0) {
            return new PublicNoteNavigationVO(null, null, 0);
        }
        long position = noteMapper.selectCount(compare(searchQuery(keyword, category, descendants), anchor, true)) + 1;
        List<Note> previous = noteMapper.selectList(compare(searchQuery(keyword, category, descendants), anchor, true)
                .orderByAsc(Note::getUpdatedAt, Note::getCreatedAt, Note::getId).last("LIMIT 1"));
        List<Note> next = noteMapper.selectList(newest(compare(searchQuery(keyword, category, descendants), anchor, false)).last("LIMIT 1"));
        return new PublicNoteNavigationVO(previous.isEmpty() ? null : item(previous.get(0), keyword),
                next.isEmpty() ? null : item(next.get(0), keyword), position);
    }

    private Note requirePublic(Long id) {
        Note note = noteMapper.selectOne(publicQuery().eq(Note::getId, id));
        if (note == null) throw BusinessException.notFound("Official note not found");
        return note;
    }

    private LambdaQueryWrapper<Note> publicQuery() {
        return new LambdaQueryWrapper<Note>().eq(Note::getPublished, true).eq(Note::getOfficial, true);
    }

    private LambdaQueryWrapper<Note> searchQuery(String keyword, String category, boolean descendants) {
        LambdaQueryWrapper<Note> query = publicQuery();
        if (category != null && !category.isBlank()) {
            String normalized = category.trim();
            if ("未分类".equals(normalized)) exactCategory(query, null);
            else if (descendants) {
                String parent = categoryPath(normalized);
                List<String> matching = noteMapper.selectObjs(publicQuery().select(Note::getCategory).groupBy(Note::getCategory)).stream()
                        .filter(value -> value instanceof String).map(Object::toString)
                        .filter(value -> categoryPath(value).equals(parent) || categoryPath(value).startsWith(parent + "/")).toList();
                if (matching.isEmpty()) query.eq(Note::getCategory, normalized);
                else query.in(Note::getCategory, matching);
            } else exactCategory(query, normalized);
        }
        if (keyword != null && !keyword.isBlank()) {
            String literal = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            query.and(q -> q.apply("LOWER(title) LIKE {0} ESCAPE '!'", literal)
                    .or().apply("LOWER(content) LIKE {0} ESCAPE '!'", literal)
                    .or().apply("LOWER(summary) LIKE {0} ESCAPE '!'", literal)
                    .or().apply("LOWER(tags) LIKE {0} ESCAPE '!'", literal)
                    .or().apply("LOWER(category) LIKE {0} ESCAPE '!'", literal));
        }
        return query;
    }

    private void exactCategory(LambdaQueryWrapper<Note> query, String category) {
        if (category == null || category.isBlank() || "未分类".equals(category)) {
            query.and(q -> q.isNull(Note::getCategory).or().eq(Note::getCategory, "").or().eq(Note::getCategory, "未分类"));
        } else query.eq(Note::getCategory, category);
    }

    private String categoryPath(String value) {
        return Arrays.stream(value.split("\\s*(?:/|>|::|\\\\)\\s*")).map(String::trim).filter(part -> !part.isEmpty()).collect(Collectors.joining("/"));
    }

    private LambdaQueryWrapper<Note> newest(LambdaQueryWrapper<Note> query) {
        return query.orderByDesc(Note::getUpdatedAt, Note::getCreatedAt, Note::getId);
    }

    // Keyset neighbors use the same deterministic ordering as paginated results.
    private LambdaQueryWrapper<Note> compare(LambdaQueryWrapper<Note> query, Note anchor, boolean newer) {
        String operator = newer ? ">" : "<";
        return query.and(q -> q.apply("updated_at " + operator + " {0}", anchor.getUpdatedAt())
                .or().apply("updated_at = {0} AND created_at " + operator + " {1}", anchor.getUpdatedAt(), anchor.getCreatedAt())
                .or().apply("updated_at = {0} AND created_at = {1} AND id " + operator + " {2}", anchor.getUpdatedAt(), anchor.getCreatedAt(), anchor.getId()));
    }

    private PublicNoteItemVO item(Note note, String keyword) {
        return new PublicNoteItemVO(note.getId(), note.getTitle(), note.getCategory(), note.getTags(),
                NoteExcerpt.extract(note.getContent(), keyword == null ? "" : keyword.trim()), note.getWordCount(), note.getUpdatedAt());
    }

    private void validate(String keyword, String category, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50 || (keyword != null && keyword.length() > 100) || (category != null && category.length() > 64)) {
            throw BusinessException.badRequest("Invalid search parameters: page >= 1, pageSize 1-50, keyword <= 100, category <= 64");
        }
    }
}

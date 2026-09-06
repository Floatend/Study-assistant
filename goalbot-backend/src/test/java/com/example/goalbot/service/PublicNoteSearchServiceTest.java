package com.example.goalbot.service;

import com.example.goalbot.common.BusinessException;
import com.example.goalbot.common.GlobalExceptionHandler;
import com.example.goalbot.controller.PublicNoteController;
import com.example.goalbot.mapper.NoteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PublicNoteSearchServiceTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.datasource.url=jdbc:h2:mem:note_search;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="})
class PublicNoteSearchServiceTest {
    @Configuration
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = NoteMapper.class)
    @Import(PublicNoteSearchService.class)
    static class TestConfig { }

    @Autowired JdbcTemplate jdbc;
    @Autowired PublicNoteSearchService service;

    @BeforeEach void seedIsolatedDatabase() {
        jdbc.execute("DROP TABLE IF EXISTS note");
        jdbc.execute("CREATE TABLE note(id BIGINT PRIMARY KEY, user_id BIGINT, title VARCHAR(160), file_name VARCHAR(200), "
                + "summary VARCHAR(1000), content LONGTEXT, tags VARCHAR(200), category VARCHAR(64), is_published BOOLEAN, "
                + "is_official BOOLEAN, word_count INT, created_at DATETIME, updated_at DATETIME)");
        for (long id = 1; id <= 137; id++) {
            String category = id <= 5 ? "课程/物理" : id <= 10 ? "课程 > 网络" : id <= 15 ? "课程::数学" : id <= 20 ? "课程\\算法" : id <= 25 ? "课程资料" : "技术/Java";
            insert(id, category, true, true, "普通内容。" + (id == 1 ? " only_body_token literal %_! " : ""));
        }
        insert(200, "课程/物理", true, false, "only_body_token");
        insert(201, "课程/物理", false, true, "only_body_token");
    }

    void insert(long id, String category, boolean published, boolean official, String content) {
        jdbc.update("INSERT INTO note(id,user_id,title,content,category,is_published,is_official,word_count,created_at,updated_at) "
                + "VALUES(?,1,?,?,?,?,?,100,'2026-09-06 12:00:00','2026-09-06 12:00:00')", id, "Note " + id, content, category, published, official);
    }

    @Test void paginatesBeyond100WithStableTieBreakingAndClampsMissingPages() {
        var first = service.search("", "", true, 1, 12);
        assertThat(first.total()).isEqualTo(137);
        assertThat(first.items()).hasSize(12);
        assertThat(first.items().get(0).id()).isEqualTo(137);
        assertThat(first.items().get(11).id()).isEqualTo(126);
        var last = service.search("", "", true, 999, 12);
        assertThat(last.page()).isEqualTo(12);
        assertThat(last.items()).extracting("id").containsExactly(5L, 4L, 3L, 2L, 1L);
    }

    @Test void findsOldBodyOnlyMatchesAndTreatsWildcardsAsLiteralText() {
        var match = service.search("ONLY_BODY_TOKEN", "", true, 1, 12);
        assertThat(match.total()).isEqualTo(1);
        assertThat(match.items().get(0).id()).isEqualTo(1);
        assertThat(match.items().get(0).excerpt()).contains("only_body_token");
        assertThat(service.search("%_!", "", true, 1, 12).total()).isEqualTo(1);
        assertThat(service.search("' OR 1=1 --", "", true, 1, 12).total()).isZero();
    }

    @Test void parentCategoryIncludesSupportedSeparatorsButNotSimilarPrefix() {
        assertThat(service.search("", "课程", true, 1, 12).total()).isEqualTo(20);
        assertThat(service.search("", "课程/物理", false, 1, 12).total()).isEqualTo(5);
        assertThat(service.search("", "课程", false, 1, 12).total()).isZero();
    }

    @Test void uncategorizedIncludesNullEmptyAndDisplayLabel() {
        insert(300, null, true, true, "内容");
        insert(301, "", true, true, "内容");
        insert(302, "未分类", true, true, "内容");
        assertThat(service.search("", "未分类", true, 1, 12).total()).isEqualTo(3);
    }

    @Test void relatedExcludesSelfDraftsAndNonOfficialNotes() {
        assertThat(service.related(1L)).extracting("id").containsExactly(5L, 4L, 3L, 2L);
        assertThatThrownBy(() -> service.related(200L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.related(201L)).isInstanceOf(BusinessException.class);
    }

    @Test void neighborsCrossPageBoundariesAndRespectSearchScope() {
        var nav = service.navigation(126L, "", "", true);
        assertThat(nav.position()).isEqualTo(12);
        assertThat(nav.previous().id()).isEqualTo(127);
        assertThat(nav.next().id()).isEqualTo(125);
        assertThat(service.navigation(1L, "only_body_token", "", true).next()).isNull();
        assertThat(service.navigation(126L, "only_body_token", "", true).position()).isZero();
        assertThatThrownBy(() -> service.navigation(201L, "", "", true)).isInstanceOf(BusinessException.class);
    }

    @Test void validatesPaginationAndQueryLength() {
        assertThatThrownBy(() -> service.search("", "", true, 0, 12)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.search("", "", true, 1, 51)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.search("a".repeat(101), "", true, 1, 12)).isInstanceOf(BusinessException.class);
    }

    @Test void exposesLightweightResultsThroughRealController() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new PublicNoteController(mock(NoteService.class), service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/public/notes/search").param("keyword", "only_body_token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist()).andExpect(jsonPath("$.data.items[0].userId").doesNotExist());
        mvc.perform(get("/api/public/notes/search").param("page", "0")).andExpect(jsonPath("$.code").value(400));
    }
}

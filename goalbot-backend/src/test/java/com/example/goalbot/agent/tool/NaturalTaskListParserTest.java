package com.example.goalbot.agent.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalTaskListParserTest {

    @Test
    void extractsEveryTaskFromTheReportedMessage() {
        List<String> titles = NaturalTaskListParser.parse("今天写高数卷子，新工科英语复习");

        assertEquals(List.of("写高数卷子", "新工科英语复习"), titles);
        assertTrue(NaturalTaskListParser.isLikelyTaskList("今天写高数卷子，新工科英语复习"));
    }

    @Test
    void leavesExplicitBatchSchedulesToTheScheduleParser() {
        List<String> titles = NaturalTaskListParser.parse(
                "创建日程，6.25，8:30-10:10，学术英语，10:50-11:50，新工科英语"
        );

        assertTrue(titles.isEmpty());
    }

    @Test
    void doesNotTreatOrdinaryCommaSeparatedChatAsATaskList() {
        assertEquals(2, NaturalTaskListParser.parse("今天天气不错，心情也很好").size());
        assertFalse(NaturalTaskListParser.isLikelyTaskList("今天天气不错，心情也很好"));
    }
}

package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.dto.checkin.CheckinCreateRequest;
import com.example.goalbot.service.CheckinService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.CheckinVO;
import com.example.goalbot.vo.TaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckinTaskToolTest {

    @Mock
    private TaskService taskService;
    @Mock
    private CheckinService checkinService;

    @InjectMocks
    private CheckinTaskTool tool;

    @Test
    void allowsCheckinWithoutExplicitDuration() {
        TaskVO task = new TaskVO();
        task.setId(8L);
        task.setTitle("上物理课");
        task.setPlannedMinutes(180);
        when(taskService.listTodayTasks(1L)).thenReturn(List.of(task));

        CheckinVO saved = new CheckinVO();
        saved.setTaskId(8L);
        saved.setActualMinutes(180);
        when(checkinService.createCheckin(eq(1L), any(CheckinCreateRequest.class))).thenReturn(saved);

        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("task_keyword", "物理");
        arguments.put("source_text", "打卡物理");
        call.setArguments(arguments);

        ToolResult result = tool.execute(1L, call);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("按任务计划"));
        ArgumentCaptor<CheckinCreateRequest> captor = ArgumentCaptor.forClass(CheckinCreateRequest.class);
        verify(checkinService).createCheckin(eq(1L), captor.capture());
        assertNull(captor.getValue().getActualMinutes());
    }
}

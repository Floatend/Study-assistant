package com.example.goalbot.service.impl;

import com.example.goalbot.dto.checkin.CheckinCreateRequest;
import com.example.goalbot.entity.Checkin;
import com.example.goalbot.entity.Task;
import com.example.goalbot.mapper.CheckinMapper;
import com.example.goalbot.mapper.TaskMapper;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.vo.CheckinVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckinServiceImplTest {

    @Mock
    private CheckinMapper checkinMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private GoalService goalService;

    private CheckinServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CheckinServiceImpl(taskMapper, goalService);
        ReflectionTestUtils.setField(service, "baseMapper", checkinMapper);
    }

    @Test
    void defaultsActualMinutesToTaskPlannedMinutes() {
        Task task = new Task();
        task.setId(8L);
        task.setUserId(1L);
        task.setTitle("上物理课");
        task.setPlannedMinutes(180);
        task.setStatus(0);
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(taskMapper.selectById(8L)).thenReturn(task);
        when(checkinMapper.insert(any(Checkin.class))).thenReturn(1);

        CheckinCreateRequest request = new CheckinCreateRequest();
        request.setTaskId(8L);

        CheckinVO result = service.createCheckin(1L, request);

        assertEquals(180, result.getActualMinutes());
        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinMapper).insert(captor.capture());
        assertEquals(180, captor.getValue().getActualMinutes());
    }
}

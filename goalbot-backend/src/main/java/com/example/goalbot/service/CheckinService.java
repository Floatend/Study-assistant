package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.checkin.CheckinCreateRequest;
import com.example.goalbot.entity.Checkin;
import com.example.goalbot.vo.CheckinStatsVO;
import com.example.goalbot.vo.CheckinVO;

import java.time.LocalDate;
import java.util.List;

public interface CheckinService extends IService<Checkin> {

    CheckinVO createCheckin(Long userId, CheckinCreateRequest request);

    List<CheckinVO> listRecent(Long userId, Integer limit);

    CheckinStatsVO getStats(Long userId, LocalDate startDate, LocalDate endDate);
}

package com.example.goalbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.goalbot.entity.Goal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoalMapper extends BaseMapper<Goal> {
}

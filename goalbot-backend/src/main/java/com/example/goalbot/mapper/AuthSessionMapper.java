package com.example.goalbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.goalbot.entity.AuthSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSession> {
}

package com.example.goalbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.goalbot.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}

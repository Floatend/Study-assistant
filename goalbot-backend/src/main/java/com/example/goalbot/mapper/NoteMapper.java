package com.example.goalbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.goalbot.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}

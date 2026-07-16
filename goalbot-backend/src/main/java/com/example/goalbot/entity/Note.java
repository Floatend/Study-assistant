package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String fileName;

    private String summary;

    private String content;

    private String tags;

    private String category;

    @TableField("is_published")
    private Boolean published;

    @TableField("is_official")
    private Boolean official;

    private Integer wordCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

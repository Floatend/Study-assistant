package com.example.goalbot.integration.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentArticle {

    private String sourceName;

    private String title;

    private String url;

    private String content;

    private String publishedAt;
}

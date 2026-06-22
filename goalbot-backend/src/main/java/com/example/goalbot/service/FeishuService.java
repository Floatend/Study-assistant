package com.example.goalbot.service;

public interface FeishuService {

    boolean sendText(String content);

    boolean sendTextToChat(String chatId, String content);

    boolean sendRichText(String title, String content);

    boolean sendRichTextToChat(String chatId, String title, String content);
}

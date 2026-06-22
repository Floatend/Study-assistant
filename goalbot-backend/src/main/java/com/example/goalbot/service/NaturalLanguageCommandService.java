package com.example.goalbot.service;

import com.example.goalbot.dto.command.CommandIntent;

public interface NaturalLanguageCommandService {

    CommandIntent parse(Long userId, String text);
}

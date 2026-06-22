package com.example.goalbot.service;

import com.example.goalbot.vo.IcsImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface IcsImportService {

    IcsImportResultVO importIcs(
            Long userId,
            MultipartFile file,
            boolean dryRun,
            LocalDate startDate,
            LocalDate endDate,
            boolean skipExisting
    );
}

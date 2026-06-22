package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.task.TaskCreateRequest;
import com.example.goalbot.dto.task.TaskUpdateRequest;
import com.example.goalbot.service.IcsImportService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.IcsImportResultVO;
import com.example.goalbot.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController extends BaseController {

    private final TaskService taskService;
    private final IcsImportService icsImportService;

    @PostMapping
    public Result<TaskVO> createTask(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody TaskCreateRequest request) {
        return Result.success(taskService.createTask(currentUserId(headerUserId), request));
    }

    @GetMapping
    public Result<List<TaskVO>> listTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long goalId,
            @RequestParam(required = false) Integer status) {
        return Result.success(taskService.listTasks(currentUserId(headerUserId), date, goalId, status));
    }

    @GetMapping("/today")
    public Result<List<TaskVO>> listTodayTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        return Result.success(taskService.listTodayTasks(currentUserId(headerUserId)));
    }

    @GetMapping("/calendar")
    public Result<List<TaskVO>> listCalendarTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(taskService.listCalendarTasks(currentUserId(headerUserId), startDate, endDate));
    }

    @PostMapping(value = "/import/ics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<IcsImportResultVO> importIcs(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean skipExisting) {
        return Result.success(icsImportService.importIcs(
                currentUserId(headerUserId),
                file,
                dryRun,
                startDate,
                endDate,
                skipExisting
        ));
    }

    @PutMapping("/{id}")
    public Result<TaskVO> updateTask(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        return Result.success(taskService.updateTask(currentUserId(headerUserId), id, request));
    }

    @PutMapping("/{id}/complete")
    public Result<TaskVO> completeTask(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id) {
        return Result.success(taskService.completeTask(currentUserId(headerUserId), id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id) {
        taskService.deleteTask(currentUserId(headerUserId), id);
        return Result.success();
    }
}

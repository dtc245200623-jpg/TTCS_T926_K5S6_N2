package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.entity.SystemLog;
import com.example.auth.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system-logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogRepository systemLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemLog>>> getSystemLogs() {
        List<SystemLog> logs = systemLogRepository.findAllByOrderByTimestampDesc();
        return ResponseEntity.ok(ApiResponse.success("Lấy nhật ký hệ thống thành công", logs));
    }
}

package com.example.auth.controller;

import com.example.auth.dto.ApplicationDto;
import com.example.auth.dto.ApiResponse;
import com.example.auth.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping("/{jobId}/apply")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationDto>> apply(
            @PathVariable Long jobId,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        
        String cvUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("FE/uploads").toAbsolutePath();
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                java.nio.file.Path filePath = uploadDir.resolve(filename);
                file.transferTo(filePath.toFile());
                cvUrl = "uploads/" + filename;
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi tải lên file CV: " + e.getMessage()));
            }
        }

        ApplicationDto dto = applicationService.applyForJob(jobId, username, cvUrl);
        return ResponseEntity.ok(ApiResponse.success("Ứng tuyển thành công", dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getMyApplications(Authentication authentication) {
        String username = authentication.getName();
        List<ApplicationDto> list = applicationService.getApplicationsByCandidate(username);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hồ sơ thành công", list));
    }

    @GetMapping
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN') or hasRole('HIRING_MANAGER')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getAllApplications() {
        List<ApplicationDto> list = applicationService.getAllApplications();
        return ResponseEntity.ok(ApiResponse.success("Thành công", list));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        ApplicationDto dto = applicationService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", dto));
    }
}

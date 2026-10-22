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
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        ApplicationDto dto = applicationService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", dto));
    }

    /**
     * KN-304, KN-306, KN-308: Lấy chi tiết hồ sơ ứng viên (có phân quyền Recruiter & Hiring Manager)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<com.example.auth.dto.CandidateDetailDto>> getCandidateDetail(
            @PathVariable Long id,
            Authentication authentication) {
        com.example.auth.dto.CandidateDetailDto detail = applicationService.getCandidateDetail(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết hồ sơ thành công", detail));
    }

    /**
     * KN-307: Lấy danh sách ghi chú nội bộ của hồ sơ
     */
    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<com.example.auth.dto.ApplicationNoteDto>>> getApplicationNotes(
            @PathVariable Long id,
            Authentication authentication) {
        List<com.example.auth.dto.ApplicationNoteDto> notes = applicationService.getApplicationNotes(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ghi chú thành công", notes));
    }

    /**
     * KN-307: Thêm ghi chú nội bộ mới cho hồ sơ
     */
    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<com.example.auth.dto.ApplicationNoteDto>> addApplicationNote(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        String content = body != null ? body.get("content") : "";
        com.example.auth.dto.ApplicationNoteDto note = applicationService.addApplicationNote(id, content, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Thêm ghi chú thành công", note));
    }

    /**
     * KN-307: Xóa ghi chú
     */
    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteApplicationNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        applicationService.deleteApplicationNote(noteId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Xóa ghi chú thành công", null));
    }

    /**
     * KN-307: Cập nhật nhãn (Tags) cho hồ sơ
     */
    @PutMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<com.example.auth.dto.CandidateDetailDto>> updateApplicationTags(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, List<String>> body,
            Authentication authentication) {
        List<String> tags = body != null ? body.get("tags") : List.of();
        com.example.auth.dto.CandidateDetailDto updated = applicationService.updateApplicationTags(id, tags, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nhãn thành công", updated));
    }
}

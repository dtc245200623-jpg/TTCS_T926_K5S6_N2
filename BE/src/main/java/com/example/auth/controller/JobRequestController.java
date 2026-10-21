package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.JobRequestDto;
import com.example.auth.service.JobRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-requests")
@RequiredArgsConstructor
public class JobRequestController {

    private final JobRequestService jobRequestService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'APPROVER', 'HR_MANAGER', 'ADMIN', 'CANDIDATE', 'RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobRequestDto>>> getAllJobRequests() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", jobRequestService.getAllJobRequests()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<JobRequestDto>>> getMyJobRequests(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu của tôi thành công", 
                jobRequestService.getMyJobRequests(authentication.getName())));
    }

    /**
     * KN-250: Lấy danh sách Recruiter phục vụ bộ lọc danh sách yêu cầu tuyển dụng
     */
    @GetMapping("/recruiters")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'APPROVER', 'HR_MANAGER', 'ADMIN', 'RECRUITER')")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getRecruiters() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách recruiter thành công", 
                jobRequestService.getAllRecruiters()));
    }

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'APPROVER', 'HR_MANAGER', 'ADMIN', 'RECRUITER')")
    public ResponseEntity<ApiResponse<JobRequestDto>> getJobRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu thành công", jobRequestService.getJobRequestById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> createJobRequest(
            @RequestParam(defaultValue = "false", required = false) boolean isDraft,
            @RequestBody JobRequestDto dto,
            Authentication authentication) {
        boolean draftMode = isDraft || Boolean.TRUE.equals(dto.getIsDraft()) || "DRAFT".equalsIgnoreCase(dto.getStatus());
        JobRequestDto created = jobRequestService.createOrDraftJobRequest(dto, authentication.getName(), draftMode);
        String msg = draftMode ? "Lưu nháp yêu cầu tuyển dụng thành công" : "Tạo và gửi yêu cầu tuyển dụng thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> updateJobRequest(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false", required = false) boolean isDraft,
            @RequestBody JobRequestDto dto,
            Authentication authentication) {
        boolean draftMode = isDraft || Boolean.TRUE.equals(dto.getIsDraft()) || "DRAFT".equalsIgnoreCase(dto.getStatus());
        JobRequestDto updated = jobRequestService.updateJobRequest(id, dto, authentication.getName(), draftMode);
        String msg = draftMode ? "Cập nhật bản nháp thành công" : "Cập nhật và gửi yêu cầu tuyển dụng thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> submitJobRequest(
            @PathVariable Long id,
            Authentication authentication) {
        JobRequestDto submitted = jobRequestService.submitJobRequest(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu tuyển dụng đi phê duyệt thành công", submitted));
    }

    @PostMapping({ "/{id}/clone", "/{id}/duplicate" })
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> cloneJobRequest(
            @PathVariable Long id,
            Authentication authentication) {
        JobRequestDto cloned = jobRequestService.cloneJobRequest(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Sao chép yêu cầu tuyển dụng thành công (Bản nháp)", cloned));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteJobRequest(
            @PathVariable Long id,
            Authentication authentication) {
        jobRequestService.deleteJobRequest(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Xóa yêu cầu tuyển dụng thành công", null));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> approveJobRequest(
            @PathVariable Long id,
            @RequestParam boolean isApproved,
            Authentication authentication) {
        JobRequestDto updated = jobRequestService.approveOrReject(id, isApproved, authentication.getName());
        String action = isApproved ? "Phê duyệt" : "Từ chối";
        return ResponseEntity.ok(ApiResponse.success(action + " yêu cầu thành công", updated));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> assignRecruiter(
            @PathVariable Long id,
            @RequestParam String recruiterUsername) {
        JobRequestDto updated = jobRequestService.assignRecruiter(id, recruiterUsername);
        return ResponseEntity.ok(ApiResponse.success("Phân công recruiter thành công", updated));
    }

    /**
     * KN-248: Kiểm tra và cảnh báo ứng viên còn trong pipeline trước khi cho phép đóng
     */
    @GetMapping("/{id}/pipeline-check")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'HIRING_MANAGER')")
    public ResponseEntity<ApiResponse<com.example.auth.dto.PipelineCheckDto>> checkCandidatePipeline(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra pipeline ứng viên thành công", 
                jobRequestService.checkCandidatePipeline(id)));
    }

    /**
     * KN-246, KN-247, KN-249: Đóng yêu cầu tuyển dụng với 3 trạng thái kết thúc có lý do riêng
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'HIRING_MANAGER')")
    public ResponseEntity<ApiResponse<JobRequestDto>> closeJobRequest(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody com.example.auth.dto.CloseJobRequestDto dto,
            Authentication authentication) {
        JobRequestDto closed = jobRequestService.closeJobRequest(id, dto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đóng yêu cầu tuyển dụng thành công", closed));
    }

    /**
     * Đăng tin & Cổng ứng tuyển - Nhân viên tuyển dụng:
     * Tạo và xuất bản tin tuyển dụng từ yêu cầu tuyển dụng đã duyệt
     */
    @PostMapping("/{id:[0-9]+}/publish")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_RECRUITER') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> publishJobPosting(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody com.example.auth.dto.PublishJobPostingDto dto,
            Authentication authentication) {
        JobRequestDto published = jobRequestService.publishJobPosting(id, dto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đăng tin tuyển dụng thành công", published));
    }
}

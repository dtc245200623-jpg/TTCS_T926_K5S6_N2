package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.CompetencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competencies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CompetencyController {

    private final CompetencyService competencyService;

    /**
     * KN-182: Lấy danh sách khung năng lực
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CompetencyFrameworkResponse>>> getAllFrameworks(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<CompetencyFrameworkResponse> list = competencyService.getAllFrameworks(activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khung năng lực thành công", list));
    }

    /**
     * Lấy chi tiết khung năng lực kèm danh sách tiêu chí và chức danh áp dụng
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompetencyFrameworkResponse>> getFrameworkById(@PathVariable Long id) {
        CompetencyFrameworkResponse response = competencyService.getFrameworkById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin khung năng lực thành công", response));
    }

    /**
     * KN-182 & KN-183: Tạo mới khung năng lực kèm tiêu chí (Tổng trọng số = 100%)
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyFrameworkResponse>> createFramework(
            @Valid @RequestBody CompetencyFrameworkRequest request) {
        CompetencyFrameworkResponse created = competencyService.createFramework(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo khung năng lực mới thành công", created));
    }

    /**
     * KN-182 & KN-183: Cập nhật thông tin khung năng lực và tiêu chí đánh giá
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyFrameworkResponse>> updateFramework(
            @PathVariable Long id,
            @Valid @RequestBody CompetencyFrameworkRequest request) {
        CompetencyFrameworkResponse updated = competencyService.updateFramework(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khung năng lực thành công", updated));
    }

    /**
     * Bật / Tắt trạng thái áp dụng khung năng lực
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyFrameworkResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active) {
        CompetencyFrameworkResponse updated = competencyService.toggleFrameworkStatus(id, active);
        String msg = Boolean.TRUE.equals(updated.getActive()) ? "Kích hoạt khung năng lực" : "Ngừng áp dụng khung năng lực";
        return ResponseEntity.ok(ApiResponse.success(msg + " thành công", updated));
    }

    /**
     * Xóa khung năng lực
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFramework(@PathVariable Long id) {
        competencyService.deleteFramework(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa khung năng lực thành công"));
    }

    /**
     * KN-184: Ánh xạ danh sách chức danh vào khung năng lực
     */
    @PostMapping("/{id}/positions")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyFrameworkResponse>> assignPositions(
            @PathVariable Long id,
            @RequestBody List<Long> positionIds) {
        CompetencyFrameworkResponse updated = competencyService.assignPositions(id, positionIds);
        return ResponseEntity.ok(ApiResponse.success("Ánh xạ chức danh vào khung năng lực thành công", updated));
    }

    /**
     * Hủy ánh xạ khung năng lực khỏi chức danh
     */
    @DeleteMapping("/positions/{positionId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unassignPosition(@PathVariable Long positionId) {
        competencyService.unassignPosition(positionId);
        return ResponseEntity.ok(ApiResponse.success("Hủy ánh xạ khung năng lực khỏi chức danh thành công"));
    }

    /**
     * KN-184: Sinh phiếu đánh giá phỏng vấn mẫu theo chức danh
     */
    @GetMapping("/evaluations/template/position/{positionId}")
    public ResponseEntity<ApiResponse<EvaluationTemplateResponse>> generateTemplateForPosition(
            @PathVariable Long positionId) {
        EvaluationTemplateResponse template = competencyService.generateEvaluationTemplateForPosition(positionId);
        return ResponseEntity.ok(ApiResponse.success("Sinh phiếu đánh giá phỏng vấn theo chức danh thành công", template));
    }

    /**
     * KN-184: Sinh phiếu đánh giá phỏng vấn cho một buổi phỏng vấn cụ thể
     */
    @GetMapping("/evaluations/template/interview/{interviewId}")
    public ResponseEntity<ApiResponse<EvaluationTemplateResponse>> generateTemplateForInterview(
            @PathVariable Long interviewId) {
        EvaluationTemplateResponse template = competencyService.generateEvaluationTemplateForInterview(interviewId);
        return ResponseEntity.ok(ApiResponse.success("Sinh phiếu đánh giá phỏng vấn thành công", template));
    }

    /**
     * Chấm điểm và lưu kết quả phỏng vấn kèm trọng số tiêu chí
     */
    @PostMapping("/evaluations")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> submitEvaluation(
            @Valid @RequestBody EvaluationSubmitRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        EvaluationDetailResponse result = competencyService.submitEvaluation(request, username);
        return ResponseEntity.ok(ApiResponse.success("Lưu phiếu đánh giá phỏng vấn thành công", result));
    }

    /**
     * Xem chi tiết kết quả đánh giá phỏng vấn
     */
    @GetMapping("/evaluations/interview/{interviewId}")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> getEvaluationByInterview(
            @PathVariable Long interviewId) {
        EvaluationDetailResponse result = competencyService.getEvaluationByInterviewId(interviewId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phiếu đánh giá thành công", result));
    }
}

package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.ApprovalActionRequest;
import com.example.auth.dto.ApprovalRuleDto;
import com.example.auth.dto.JobRequestApprovalStepDto;
import com.example.auth.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    // ==========================================
    // CẤU HÌNH CHUỖI CẤP DUYỆT (KN-220, KN-222)
    // ==========================================

    @GetMapping({"/approval-rules", "/approval-workflows"})
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'APPROVER') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_APPROVER')")
    public ResponseEntity<ApiResponse<List<ApprovalRuleDto>>> getAllRules() {
        List<ApprovalRuleDto> list = approvalWorkflowService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quy tắc duyệt thành công", list));
    }

    @GetMapping({"/approval-rules/{id}", "/approval-workflows/{id}"})
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'APPROVER') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_APPROVER')")
    public ResponseEntity<ApiResponse<ApprovalRuleDto>> getRuleById(@PathVariable Long id) {
        ApprovalRuleDto dto = approvalWorkflowService.getRuleById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết quy tắc duyệt thành công", dto));
    }

    @PostMapping({"/approval-rules", "/approval-workflows"})
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ApprovalRuleDto>> createRule(
            @RequestBody ApprovalRuleDto dto,
            Authentication authentication) {
        ApprovalRuleDto created = approvalWorkflowService.createRule(dto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Tạo quy tắc chuỗi duyệt thành công", created));
    }

    @PutMapping({"/approval-rules/{id}", "/approval-workflows/{id}"})
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ApprovalRuleDto>> updateRule(
            @PathVariable Long id,
            @RequestBody ApprovalRuleDto dto,
            Authentication authentication) {
        ApprovalRuleDto updated = approvalWorkflowService.updateRule(id, dto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quy tắc chuỗi duyệt thành công", updated));
    }

    @DeleteMapping({"/approval-rules/{id}", "/approval-workflows/{id}"})
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        approvalWorkflowService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa quy tắc chuỗi duyệt thành công", null));
    }

    // =======================================================
    // THEO DÕI VÀ PHÊ DUYỆT TỪNG BƯỚC JOB REQUEST (KN-235, KN-221)
    // =======================================================

    @GetMapping({"/job-requests/{id}/approval-steps", "/approval-workflows/job-requests/{id}/steps"})
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'HR_MANAGER', 'APPROVER', 'ADMIN', 'RECRUITER') or hasAuthority('ROLE_HIRING_MANAGER') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_APPROVER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobRequestApprovalStepDto>>> getJobRequestApprovalSteps(
            @PathVariable Long id) {
        List<JobRequestApprovalStepDto> steps = approvalWorkflowService.getApprovalStepsForJobRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách các bước duyệt thành công", steps));
    }

    @PostMapping({"/job-requests/{id}/approval-steps/{stepId}/action", "/approval-workflows/job-requests/{id}/steps/{stepId}/action"})
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'APPROVER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_APPROVER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestApprovalStepDto>> processStepAction(
            @PathVariable Long id,
            @PathVariable Long stepId,
            @RequestBody(required = false) ApprovalActionRequest request,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean isApproved,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        String finalAction = "APPROVE";
        String actionComment = "";

        if (request != null) {
            if (request.getAction() != null && !request.getAction().trim().isEmpty()) {
                finalAction = request.getAction().trim().toUpperCase();
            } else if (request.getApproved() != null) {
                finalAction = Boolean.TRUE.equals(request.getApproved()) ? "APPROVE" : "REJECT";
            }
            actionComment = request.getComment();
        } else {
            if (action != null && !action.trim().isEmpty()) {
                finalAction = action.trim().toUpperCase();
            } else if (isApproved != null) {
                finalAction = Boolean.TRUE.equals(isApproved) ? "APPROVE" : "REJECT";
            }
            actionComment = comment;
        }

        JobRequestApprovalStepDto result = approvalWorkflowService.processApprovalStep(
                id, stepId, finalAction, actionComment, authentication.getName());

        String actionStr;
        switch (finalAction) {
            case "APPROVE":
            case "APPROVED":
                actionStr = "Phê duyệt";
                break;
            case "REJECT":
            case "REJECTED":
                actionStr = "Từ chối";
                break;
            case "REQUEST_CHANGE":
            case "CHANGES_REQUESTED":
            case "REQUEST_INFO":
                actionStr = "Yêu cầu bổ sung";
                break;
            default:
                actionStr = "Xử lý";
        }
        return ResponseEntity.ok(ApiResponse.success(actionStr + " bước duyệt thành công", result));
    }
}

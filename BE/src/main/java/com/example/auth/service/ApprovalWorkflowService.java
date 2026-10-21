package com.example.auth.service;

import com.example.auth.dto.ApprovalRuleDto;
import com.example.auth.dto.JobRequestApprovalStepDto;
import com.example.auth.entity.JobRequest;

import java.util.List;

public interface ApprovalWorkflowService {

    // Quản lý quy tắc chuỗi cấp duyệt (KN-220, KN-222)
    ApprovalRuleDto createRule(ApprovalRuleDto dto, String username);
    ApprovalRuleDto updateRule(Long id, ApprovalRuleDto dto, String username);
    void deleteRule(Long id);
    ApprovalRuleDto getRuleById(Long id);
    List<ApprovalRuleDto> getAllRules();
    List<ApprovalRuleDto> getRulesByDepartment(Long departmentId);

    // Tìm kiếm quy tắc phù hợp nhất
    ApprovalRuleDto findMatchingRule(Long departmentId, Double proposedSalary);

    // Khởi tạo snapshot chuỗi cấp duyệt cho JobRequest khi gửi duyệt (KN-220, KN-221, KN-235)
    List<JobRequestApprovalStepDto> createApprovalStepsForJobRequest(JobRequest jobRequest);

    // Lấy danh sách các bước duyệt snapshot của một JobRequest (KN-235)
    List<JobRequestApprovalStepDto> getApprovalStepsForJobRequest(Long jobRequestId);

    // Xử lý hành động phê duyệt / từ chối / yêu cầu bổ sung theo bước (KN-236, KN-237, KN-238, KN-239, KN-240, KN-241)
    JobRequestApprovalStepDto processApprovalStep(Long jobRequestId, Long stepId, String action, String comment, String username);
    JobRequestApprovalStepDto processApprovalStep(Long jobRequestId, Long stepId, boolean isApproved, String comment, String username);
}

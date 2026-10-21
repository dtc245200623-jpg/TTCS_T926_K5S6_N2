package com.example.auth.service.impl;

import com.example.auth.dto.ApprovalRuleDto;
import com.example.auth.dto.ApprovalRuleStepDto;
import com.example.auth.dto.JobRequestApprovalStepDto;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalRuleRepository approvalRuleRepository;
    private final ApprovalRuleStepRepository approvalRuleStepRepository;
    private final JobRequestApprovalStepRepository jobRequestApprovalStepRepository;
    private final JobRequestRepository jobRequestRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApprovalRuleDto createRule(ApprovalRuleDto dto, String username) {
        validateRuleDto(dto);

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        ApprovalRule rule = ApprovalRule.builder()
                .name(dto.getName())
                .department(department)
                .minSalary(dto.getMinSalary())
                .maxSalary(dto.getMaxSalary())
                .budgetLimit(dto.getBudgetLimit())
                .autoEscalateOverBudget(dto.getAutoEscalateOverBudget() == null ? true : dto.getAutoEscalateOverBudget())
                .overBudgetStepName(dto.getOverBudgetStepName() != null ? dto.getOverBudgetStepName() : "Ban Giám đốc phê duyệt vượt hạn mức")
                .overBudgetRole(dto.getOverBudgetRole() != null ? dto.getOverBudgetRole() : "ROLE_ADMIN")
                .active(dto.getActive() == null ? true : dto.getActive())
                .priority(dto.getPriority() == null ? (department != null ? 10 : 0) : dto.getPriority())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ApprovalRule savedRule = approvalRuleRepository.save(rule);

        // Lưu các bước duyệt
        List<ApprovalRuleStep> stepEntities = buildStepEntities(savedRule, dto.getSteps());
        savedRule.setSteps(stepEntities);
        approvalRuleRepository.save(savedRule);

        return mapToDto(savedRule);
    }

    @Override
    @Transactional
    public ApprovalRuleDto updateRule(Long id, ApprovalRuleDto dto, String username) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new AppException("Quy tắc chuỗi duyệt không tồn tại"));

        validateRuleDto(dto);

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        rule.setName(dto.getName());
        rule.setDepartment(department);
        rule.setMinSalary(dto.getMinSalary());
        rule.setMaxSalary(dto.getMaxSalary());
        rule.setBudgetLimit(dto.getBudgetLimit());
        if (dto.getAutoEscalateOverBudget() != null) {
            rule.setAutoEscalateOverBudget(dto.getAutoEscalateOverBudget());
        }
        if (dto.getOverBudgetStepName() != null) {
            rule.setOverBudgetStepName(dto.getOverBudgetStepName());
        }
        if (dto.getOverBudgetRole() != null) {
            rule.setOverBudgetRole(dto.getOverBudgetRole());
        }
        if (dto.getActive() != null) {
            rule.setActive(dto.getActive());
        }
        if (dto.getPriority() != null) {
            rule.setPriority(dto.getPriority());
        }
        rule.setDescription(dto.getDescription());
        rule.setUpdatedAt(LocalDateTime.now());

        // Cập nhật các bước duyệt của rule
        rule.getSteps().clear();
        approvalRuleRepository.flush();

        List<ApprovalRuleStep> newSteps = buildStepEntities(rule, dto.getSteps());
        rule.getSteps().addAll(newSteps);

        ApprovalRule saved = approvalRuleRepository.save(rule);
        // Lưu ý KN-235: Việc cập nhật rule này không làm thay đổi các bản ghi JobRequestApprovalStep của các yêu cầu đang chạy
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new AppException("Quy tắc chuỗi duyệt không tồn tại"));
        approvalRuleRepository.delete(rule);
    }

    @Override
    public ApprovalRuleDto getRuleById(Long id) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new AppException("Quy tắc chuỗi duyệt không tồn tại"));
        return mapToDto(rule);
    }

    @Override
    public List<ApprovalRuleDto> getAllRules() {
        checkAndInitDefaultRule();
        return approvalRuleRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalRuleDto> getRulesByDepartment(Long departmentId) {
        checkAndInitDefaultRule();
        if (departmentId == null) {
            return approvalRuleRepository.findByDepartmentIsNullAndActiveTrue().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }
        return approvalRuleRepository.findByDepartmentIdAndActiveTrue(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApprovalRuleDto findMatchingRule(Long departmentId, Double proposedSalary) {
        ApprovalRule rule = findMatchingRuleEntity(departmentId, proposedSalary);
        return mapToDto(rule);
    }

    @Override
    @Transactional
    public List<JobRequestApprovalStepDto> createApprovalStepsForJobRequest(JobRequest jobRequest) {
        // KN-245: Ngăn sửa và xoá lịch sử phê duyệt sau khi đã ghi nhận
        List<JobRequestApprovalStep> existingSteps = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(jobRequest.getId());
        boolean hasActionedSteps = existingSteps.stream()
                .anyMatch(s -> s.getApprovalDate() != null || "APPROVED".equalsIgnoreCase(s.getStatus()) 
                        || "REJECTED".equalsIgnoreCase(s.getStatus()) || "REQUEST_CHANGE".equalsIgnoreCase(s.getStatus()));
        if (hasActionedSteps) {
            log.info("JobRequest #{} already has recorded approval history, preserving immutable audit steps (KN-245)", jobRequest.getId());
            return existingSteps.stream().map(this::mapStepToDto).collect(Collectors.toList());
        }

        // Xóa snapshot cũ nếu có trước đó
        jobRequestApprovalStepRepository.deleteByJobRequestId(jobRequest.getId());
        jobRequestApprovalStepRepository.flush();

        Double proposedSalary = jobRequest.getMaxSalary() != null ? jobRequest.getMaxSalary() : jobRequest.getMinSalary();
        Long deptId = jobRequest.getDepartment() != null ? jobRequest.getDepartment().getId() : null;

        ApprovalRule rule = findMatchingRuleEntity(deptId, proposedSalary);

        List<JobRequestApprovalStep> stepsToSave = new ArrayList<>();
        int currentOrder = 1;

        // 1. Sao chép các bước duyệt chuẩn từ rule (KN-220, KN-235)
        for (ApprovalRuleStep ruleStep : rule.getSteps()) {
            JobRequestApprovalStep snapshotStep = JobRequestApprovalStep.builder()
                    .jobRequest(jobRequest)
                    .stepOrder(currentOrder)
                    .stepName(ruleStep.getStepName())
                    .approverRole(ruleStep.getApproverRole())
                    .approverUser(ruleStep.getApproverUser())
                    .status(currentOrder == 1 ? "PENDING" : "WAITING")
                    .isOverBudgetStep(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            stepsToSave.add(snapshotStep);
            currentOrder++;
        }

        // 2. KN-221: Yêu cầu vượt hạn mức tự động thêm cấp duyệt cao hơn
        boolean isOverBudget = false;

        // Vượt trần chức danh
        if (jobRequest.getPosition() != null && jobRequest.getPosition().getMaxSalary() != null
                && proposedSalary != null && proposedSalary > jobRequest.getPosition().getMaxSalary()) {
            isOverBudget = true;
        }

        // Vượt ngân sách quy định của rule
        if (rule.getBudgetLimit() != null && proposedSalary != null && proposedSalary > rule.getBudgetLimit()) {
            isOverBudget = true;
        }

        // Vượt trần dải lương của rule
        if (rule.getMaxSalary() != null && proposedSalary != null && proposedSalary > rule.getMaxSalary()) {
            isOverBudget = true;
        }

        if (isOverBudget && Boolean.TRUE.equals(rule.getAutoEscalateOverBudget())) {
            String escalateStepName = rule.getOverBudgetStepName() != null && !rule.getOverBudgetStepName().trim().isEmpty()
                    ? rule.getOverBudgetStepName()
                    : "Ban Giám đốc phê duyệt vượt hạn mức";
            String escalateRole = rule.getOverBudgetRole() != null && !rule.getOverBudgetRole().trim().isEmpty()
                    ? rule.getOverBudgetRole()
                    : "ROLE_ADMIN";

            JobRequestApprovalStep escalateStep = JobRequestApprovalStep.builder()
                    .jobRequest(jobRequest)
                    .stepOrder(currentOrder)
                    .stepName(escalateStepName)
                    .approverRole(escalateRole)
                    .status("WAITING")
                    .isOverBudgetStep(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            stepsToSave.add(escalateStep);
        }

        List<JobRequestApprovalStep> savedSteps = jobRequestApprovalStepRepository.saveAll(stepsToSave);

        jobRequest.setCurrentApprovalStep(1);
        jobRequest.setTotalApprovalSteps(savedSteps.size());
        jobRequest.setApprovalRuleId(rule.getId());
        jobRequestRepository.save(jobRequest);

        return savedSteps.stream().map(this::mapStepToDto).collect(Collectors.toList());
    }

    @Override
    public List<JobRequestApprovalStepDto> getApprovalStepsForJobRequest(Long jobRequestId) {
        return jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(jobRequestId)
                .stream()
                .map(this::mapStepToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobRequestApprovalStepDto processApprovalStep(Long jobRequestId, Long stepId, boolean isApproved, String comment, String username) {
        return processApprovalStep(jobRequestId, stepId, isApproved ? "APPROVE" : "REJECT", comment, username);
    }

    @Override
    @Transactional
    public JobRequestApprovalStepDto processApprovalStep(Long jobRequestId, Long stepId, String action, String comment, String username) {
        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        JobRequestApprovalStep step = jobRequestApprovalStepRepository.findById(stepId)
                .orElseThrow(() -> new AppException("Bước duyệt không tồn tại"));

        if (!step.getJobRequest().getId().equals(jobRequestId)) {
            throw new AppException("Bước duyệt không thuộc về yêu cầu tuyển dụng này!");
        }

        if (!"PENDING".equalsIgnoreCase(step.getStatus())) {
            throw new AppException("Bước duyệt này đã được xử lý (trạng thái: " + step.getStatus() + ") và không thể chỉnh sửa lại lịch sử phê duyệt (KN-245)!");
        }

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        // Kiểm tra quyền duyệt
        boolean hasPermission = false;
        if (currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_ADMIN")) {
            hasPermission = true;
        } else if (step.getApproverUser() != null && step.getApproverUser().getId().equals(currentUser.getId())) {
            hasPermission = true;
        } else if (step.getApproverRole() != null && currentUser.getRoles() != null && currentUser.getRoles().contains(step.getApproverRole())) {
            hasPermission = true;
        }

        if (!hasPermission) {
            String roleName = step.getApproverRole() != null ? step.getApproverRole() : "Người duyệt chỉ định";
            throw new AppException("Bạn không có quyền thực hiện phê duyệt cho bước này (" + step.getStepName() + " - Yêu cầu: " + roleName + ")!");
        }

        // KN-236: Chuẩn hóa 3 hành động: Duyệt (APPROVE), Từ chối (REJECT), Yêu cầu bổ sung (REQUEST_CHANGE)
        String act = action != null ? action.trim().toUpperCase() : "APPROVE";

        // KN-237: Bắt buộc nhập ý kiến khi Từ chối hoặc Yêu cầu bổ sung
        if ("REJECT".equals(act) || "REJECTED".equals(act)) {
            if (comment == null || comment.trim().isEmpty()) {
                throw new AppException("Bắt buộc phải nhập ý kiến/lý do khi từ chối yêu cầu tuyển dụng!");
            }
        } else if ("REQUEST_CHANGE".equals(act) || "CHANGES_REQUESTED".equals(act) || "REQUEST_INFO".equals(act) || "ADDITIONAL_INFO".equals(act)) {
            if (comment == null || comment.trim().isEmpty()) {
                throw new AppException("Bắt buộc phải nhập ý kiến/nội dung khi yêu cầu bổ sung thông tin!");
            }
        }

        // KN-241: Ghi nhận lịch sử xử lý phê duyệt cho từng hành động
        step.setApprovalDate(LocalDateTime.now());
        step.setActionBy(currentUser);
        step.setComment(comment != null ? comment.trim() : null);

        if ("APPROVE".equals(act) || "APPROVED".equals(act)) {
            // KN-236: Hành động Duyệt
            step.setStatus("APPROVED");
            jobRequestApprovalStepRepository.save(step);

            // KN-238: Tự động chuyển yêu cầu sang cấp phê duyệt kế tiếp khi duyệt
            int nextOrder = step.getStepOrder() + 1;
            Optional<JobRequestApprovalStep> nextStepOpt = jobRequestApprovalStepRepository.findByJobRequestIdAndStepOrder(jobRequestId, nextOrder);

            if (nextStepOpt.isPresent()) {
                JobRequestApprovalStep nextStep = nextStepOpt.get();
                nextStep.setStatus("PENDING");
                jobRequestApprovalStepRepository.save(nextStep);

                jobRequest.setCurrentApprovalStep(nextOrder);
                jobRequest.setStatus("PENDING_APPROVAL");
            } else {
                // KN-239: Cập nhật trạng thái sang Đã duyệt khi cấp cuối cùng duyệt
                jobRequest.setStatus("APPROVED");
                jobRequest.setCurrentApprovalStep(step.getStepOrder());
            }
        } else if ("REJECT".equals(act) || "REJECTED".equals(act)) {
            // KN-236: Hành động Từ chối
            step.setStatus("REJECTED");
            jobRequestApprovalStepRepository.save(step);

            // Huỷ các bước còn lại
            List<JobRequestApprovalStep> allSteps = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(jobRequestId);
            for (JobRequestApprovalStep s : allSteps) {
                if ("WAITING".equalsIgnoreCase(s.getStatus())) {
                    s.setStatus("CANCELLED");
                    jobRequestApprovalStepRepository.save(s);
                }
            }

            jobRequest.setStatus("REJECTED");
        } else if ("REQUEST_CHANGE".equals(act) || "CHANGES_REQUESTED".equals(act) || "REQUEST_INFO".equals(act) || "ADDITIONAL_INFO".equals(act)) {
            // KN-236: Hành động Yêu cầu bổ sung
            step.setStatus("REQUEST_CHANGE");
            jobRequestApprovalStepRepository.save(step);

            // KN-240: Trả hồ sơ về cho người tạo khi chọn Yêu cầu bổ sung và giữ nguyên lịch sử
            jobRequest.setStatus("CHANGES_REQUESTED");
        } else {
            throw new AppException("Hành động không hợp lệ: " + action + ". Chấp nhận: APPROVE, REJECT, REQUEST_CHANGE");
        }

        jobRequest.setUpdatedAt(LocalDateTime.now());
        jobRequestRepository.save(jobRequest);

        return mapStepToDto(step);
    }

    // ==========================================
    // VALIDATION & HELPERS (KN-222)
    // ==========================================

    private void validateRuleDto(ApprovalRuleDto dto) {
        if (dto == null) {
            throw new AppException("Dữ liệu quy tắc duyệt không được để trống!");
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new AppException("Tên chuỗi cấp duyệt không được để trống!");
        }

        // KN-222: Cấu hình sai (không có cấp duyệt nào khớp / rỗng cấp duyệt) bị chặn lưu
        if (dto.getSteps() == null || dto.getSteps().isEmpty()) {
            throw new AppException("Cấu hình chuỗi cấp duyệt không hợp lệ: Phải có ít nhất 1 cấp duyệt!");
        }

        // Kiểm tra từng bước duyệt
        Set<Integer> existingOrders = new HashSet<>();
        for (int i = 0; i < dto.getSteps().size(); i++) {
            ApprovalRuleStepDto step = dto.getSteps().get(i);
            int displayIdx = i + 1;

            if (step.getStepName() == null || step.getStepName().trim().isEmpty()) {
                throw new AppException("Cấu hình không hợp lệ: Tên cấp duyệt #" + displayIdx + " không được để trống!");
            }

            boolean hasRole = step.getApproverRole() != null && !step.getApproverRole().trim().isEmpty();
            boolean hasUser = step.getApproverUserId() != null;
            if (!hasRole && !hasUser) {
                throw new AppException("Cấu hình không hợp lệ: Cấp duyệt #" + displayIdx + " (\"" + step.getStepName() + "\") chưa được phân công người hoặc vai trò phê duyệt!");
            }

            Integer order = step.getStepOrder() != null ? step.getStepOrder() : displayIdx;
            if (!existingOrders.add(order)) {
                throw new AppException("Cấu hình không hợp lệ: Thứ tự cấp duyệt bị trùng lặp ở bước #" + order);
            }
        }

        // Kiểm tra dải lương
        if (dto.getMinSalary() != null && dto.getMinSalary() < 0) {
            throw new AppException("Lương tối thiểu của quy tắc không được âm!");
        }
        if (dto.getMaxSalary() != null && dto.getMaxSalary() < 0) {
            throw new AppException("Lương tối đa của quy tắc không được âm!");
        }
        if (dto.getMinSalary() != null && dto.getMaxSalary() != null && dto.getMinSalary() > dto.getMaxSalary()) {
            throw new AppException("Cấu hình dải lương không hợp lệ: Lương tối thiểu không được lớn hơn lương tối đa!");
        }
        if (dto.getBudgetLimit() != null && dto.getBudgetLimit() < 0) {
            throw new AppException("Hạn mức ngân sách không được âm!");
        }
    }

    private List<ApprovalRuleStep> buildStepEntities(ApprovalRule rule, List<ApprovalRuleStepDto> dtos) {
        List<ApprovalRuleStep> result = new ArrayList<>();
        if (dtos == null) return result;

        int autoOrder = 1;
        for (ApprovalRuleStepDto stepDto : dtos) {
            User approverUser = null;
            if (stepDto.getApproverUserId() != null) {
                approverUser = userRepository.findById(stepDto.getApproverUserId()).orElse(null);
            }

            ApprovalRuleStep entity = ApprovalRuleStep.builder()
                    .rule(rule)
                    .stepOrder(stepDto.getStepOrder() != null ? stepDto.getStepOrder() : autoOrder)
                    .stepName(stepDto.getStepName().trim())
                    .approverRole(stepDto.getApproverRole() != null ? stepDto.getApproverRole().trim() : null)
                    .approverUser(approverUser)
                    .description(stepDto.getDescription())
                    .build();
            result.add(entity);
            autoOrder++;
        }
        return result;
    }

    private ApprovalRule findMatchingRuleEntity(Long departmentId, Double proposedSalary) {
        checkAndInitDefaultRule();
        List<ApprovalRule> allActiveRules = approvalRuleRepository.findByActiveTrueOrderByPriorityDescCreatedAtDesc();

        // 1. Thử tìm rule có cả department khớp và dải lương khớp
        if (departmentId != null) {
            for (ApprovalRule r : allActiveRules) {
                if (r.getDepartment() != null && r.getDepartment().getId().equals(departmentId)) {
                    if (isSalaryMatching(r, proposedSalary)) {
                        return r;
                    }
                }
            }
        }

        // 2. Thử tìm rule cho department khớp (không giới hạn lương)
        if (departmentId != null) {
            for (ApprovalRule r : allActiveRules) {
                if (r.getDepartment() != null && r.getDepartment().getId().equals(departmentId)) {
                    return r;
                }
            }
        }

        // 3. Thử tìm rule áp dụng chung (department == null) và dải lương khớp
        for (ApprovalRule r : allActiveRules) {
            if (r.getDepartment() == null && isSalaryMatching(r, proposedSalary)) {
                return r;
            }
        }

        // 4. Lấy rule mặc định bất kỳ còn active
        if (!allActiveRules.isEmpty()) {
            return allActiveRules.get(0);
        }

        // 5. Nếu không còn gì, tạo rule mặc định
        return createDefaultCompanyRule();
    }

    private boolean isSalaryMatching(ApprovalRule rule, Double salary) {
        if (salary == null) return true;
        if (rule.getMinSalary() != null && salary < rule.getMinSalary()) return false;
        if (rule.getMaxSalary() != null && salary > rule.getMaxSalary()) return false;
        return true;
    }

    private synchronized void checkAndInitDefaultRule() {
        if (approvalRuleRepository.count() == 0) {
            createDefaultCompanyRule();
        }
    }

    private ApprovalRule createDefaultCompanyRule() {
        ApprovalRule defaultRule = ApprovalRule.builder()
                .name("Quy trình phê duyệt tuyển dụng tiêu chuẩn toàn công ty")
                .department(null)
                .minSalary(null)
                .maxSalary(null)
                .budgetLimit(30_000_000.0)
                .autoEscalateOverBudget(true)
                .overBudgetStepName("Ban Giám đốc phê duyệt vượt hạn mức (KN-221)")
                .overBudgetRole("ROLE_ADMIN")
                .active(true)
                .priority(0)
                .description("Quy trình chuẩn áp dụng cho tất cả phòng ban khi không có quy tắc riêng")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ApprovalRule saved = approvalRuleRepository.save(defaultRule);

        List<ApprovalRuleStep> steps = new ArrayList<>();
        steps.add(ApprovalRuleStep.builder()
                .rule(saved)
                .stepOrder(1)
                .stepName("Trưởng phòng Nhân sự thẩm định")
                .approverRole("ROLE_HR_MANAGER")
                .description("Kiểm tra định biên và dải lương")
                .build());

        steps.add(ApprovalRuleStep.builder()
                .rule(saved)
                .stepOrder(2)
                .stepName("Người duyệt phê duyệt yêu cầu")
                .approverRole("ROLE_APPROVER")
                .description("Phê duyệt chính thức")
                .build());

        saved.setSteps(steps);
        return approvalRuleRepository.save(saved);
    }

    private ApprovalRuleDto mapToDto(ApprovalRule entity) {
        if (entity == null) return null;
        List<ApprovalRuleStepDto> stepDtos = entity.getSteps() != null ?
                entity.getSteps().stream().map(this::mapRuleStepToDto).collect(Collectors.toList()) :
                Collections.emptyList();

        return ApprovalRuleDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : "Áp dụng tất cả phòng ban")
                .departmentCode(entity.getDepartment() != null ? entity.getDepartment().getCode() : "ALL")
                .minSalary(entity.getMinSalary())
                .maxSalary(entity.getMaxSalary())
                .budgetLimit(entity.getBudgetLimit())
                .autoEscalateOverBudget(entity.getAutoEscalateOverBudget())
                .overBudgetStepName(entity.getOverBudgetStepName())
                .overBudgetRole(entity.getOverBudgetRole())
                .active(entity.getActive())
                .priority(entity.getPriority())
                .description(entity.getDescription())
                .steps(stepDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ApprovalRuleStepDto mapRuleStepToDto(ApprovalRuleStep entity) {
        if (entity == null) return null;
        return ApprovalRuleStepDto.builder()
                .id(entity.getId())
                .stepOrder(entity.getStepOrder())
                .stepName(entity.getStepName())
                .approverRole(entity.getApproverRole())
                .approverUserId(entity.getApproverUser() != null ? entity.getApproverUser().getId() : null)
                .approverUserName(entity.getApproverUser() != null ? entity.getApproverUser().getUsername() : null)
                .approverUserFullName(entity.getApproverUser() != null ? entity.getApproverUser().getFullName() : null)
                .description(entity.getDescription())
                .build();
    }

    private JobRequestApprovalStepDto mapStepToDto(JobRequestApprovalStep entity) {
        if (entity == null) return null;
        return JobRequestApprovalStepDto.builder()
                .id(entity.getId())
                .jobRequestId(entity.getJobRequest() != null ? entity.getJobRequest().getId() : null)
                .stepOrder(entity.getStepOrder())
                .stepName(entity.getStepName())
                .approverRole(entity.getApproverRole())
                .approverUserId(entity.getApproverUser() != null ? entity.getApproverUser().getId() : null)
                .approverUserName(entity.getApproverUser() != null ? entity.getApproverUser().getUsername() : null)
                .approverUserFullName(entity.getApproverUser() != null ? entity.getApproverUser().getFullName() : null)
                .status(entity.getStatus())
                .isOverBudgetStep(entity.getIsOverBudgetStep())
                .approvalDate(entity.getApprovalDate())
                .comment(entity.getComment())
                .actionByUserId(entity.getActionBy() != null ? entity.getActionBy().getId() : null)
                .actionByUserName(entity.getActionBy() != null ? entity.getActionBy().getUsername() : null)
                .actionByUserFullName(entity.getActionBy() != null ? entity.getActionBy().getFullName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

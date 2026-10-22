package com.example.auth.service.impl;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.ApprovalWorkflowService;
import com.example.auth.service.JobRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class JobRequestServiceImpl implements JobRequestService {

    private final JobRequestRepository jobRequestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final JobRequestApprovalStepRepository jobRequestApprovalStepRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruitmentCategoryRepository recruitmentCategoryRepository;
    @Lazy
    private final ApprovalWorkflowService approvalWorkflowService;
    @Lazy
    private final com.example.auth.service.DepartmentHeadcountService departmentHeadcountService;

    @Override
    @Transactional
    public JobRequestDto createOrDraftJobRequest(JobRequestDto dto, String username, boolean isDraft) {
        User hiringManager = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        JobPosition position = null;
        if (dto.getPositionId() != null) {
            position = jobPositionRepository.findById(dto.getPositionId()).orElse(null);
        }

        // Tự động gán phòng ban từ chức danh nếu chưa chọn
        if (dto.getDepartmentId() == null && position != null && position.getDepartment() != null) {
            dto.setDepartmentId(position.getDepartment().getId());
        }

        // Tự động gán tiêu đề từ tên chức danh nếu để trống
        if ((dto.getTitle() == null || dto.getTitle().trim().isEmpty()) && position != null) {
            dto.setTitle(position.getName());
        }

        validateJobRequest(dto, position, isDraft);

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        User recruiter = null;
        if (dto.getRecruiterId() != null) {
            recruiter = userRepository.findById(dto.getRecruiterId()).orElse(null);
        }

        JobRequest jobRequest = JobRequest.builder()
                .title(dto.getTitle())
                .department(department)
                .position(position)
                .hiringManager(hiringManager)
                .recruiter(recruiter)
                .headcount(dto.getHeadcount())
                .recruitmentReason(dto.getRecruitmentReason())
                .neededDate(dto.getNeededDate())
                .minSalary(dto.getMinSalary())
                .maxSalary(dto.getMaxSalary())
                .salaryExplanation(dto.getSalaryExplanation())
                .description(dto.getDescription())
                .requirements(dto.getRequirements())
                .status(isDraft ? "DRAFT" : "PENDING_APPROVAL")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // KN-224, KN-225, KN-226: Kiểm tra chỉ tiêu headcount nếu không phải là bản nháp
        if (!isDraft) {
            boolean isConfirmed = Boolean.TRUE.equals(dto.getIsOverHeadcountConfirmed()) || Boolean.TRUE.equals(dto.getIsOverHeadcount());
            departmentHeadcountService.validateAndProcessJobRequestHeadcount(
                    jobRequest, isConfirmed, dto.getOverHeadcountReason(), hiringManager);
        }

        JobRequest saved = jobRequestRepository.save(jobRequest);

        // KN-227: Ghi audit log nếu có ghi đè headcount
        if (!isDraft && Boolean.TRUE.equals(saved.getIsOverHeadcount())) {
            departmentHeadcountService.recordInitialOverrideAudit(saved, dto.getOverHeadcountReason(), hiringManager);
        }

        // KN-220, KN-221, KN-235: Nếu không phải lưu nháp, khởi tạo chuỗi cấp duyệt snapshot
        if (!isDraft) {
            approvalWorkflowService.createApprovalStepsForJobRequest(saved);
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public JobRequestDto updateJobRequest(Long id, JobRequestDto dto, String username, boolean isDraft) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        checkModifyPermission(jobRequest, username);

        if ("APPROVED".equalsIgnoreCase(jobRequest.getStatus()) || 
            "OPEN".equalsIgnoreCase(jobRequest.getStatus()) || 
            "CLOSED".equalsIgnoreCase(jobRequest.getStatus())) {
            throw new AppException("Không thể chỉnh sửa yêu cầu đã được phê duyệt hoặc đang mở tuyển!");
        }

        JobPosition position = null;
        if (dto.getPositionId() != null) {
            position = jobPositionRepository.findById(dto.getPositionId()).orElse(null);
        } else if (jobRequest.getPosition() != null) {
            position = jobRequest.getPosition();
            dto.setPositionId(position.getId());
        }

        if (dto.getDepartmentId() == null && position != null && position.getDepartment() != null) {
            dto.setDepartmentId(position.getDepartment().getId());
        }

        if ((dto.getTitle() == null || dto.getTitle().trim().isEmpty()) && position != null) {
            dto.setTitle(position.getName());
        }

        validateJobRequest(dto, position, isDraft);

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
            jobRequest.setDepartment(department);
        }
        if (position != null) {
            jobRequest.setPosition(position);
        }
        if (dto.getRecruiterId() != null) {
            User recruiter = userRepository.findById(dto.getRecruiterId()).orElse(null);
            jobRequest.setRecruiter(recruiter);
        }

        jobRequest.setTitle(dto.getTitle());
        jobRequest.setHeadcount(dto.getHeadcount());
        jobRequest.setRecruitmentReason(dto.getRecruitmentReason());
        jobRequest.setNeededDate(dto.getNeededDate());
        jobRequest.setMinSalary(dto.getMinSalary());
        jobRequest.setMaxSalary(dto.getMaxSalary());
        jobRequest.setSalaryExplanation(dto.getSalaryExplanation());
        jobRequest.setDescription(dto.getDescription());
        jobRequest.setRequirements(dto.getRequirements());
        boolean wasChangesRequested = "CHANGES_REQUESTED".equalsIgnoreCase(jobRequest.getStatus()) 
                || "REQUEST_CHANGE".equalsIgnoreCase(jobRequest.getStatus());

        if (isDraft) {
            jobRequest.setStatus(wasChangesRequested ? "CHANGES_REQUESTED" : "DRAFT");
        } else {
            jobRequest.setStatus("PENDING_APPROVAL");
        }
        jobRequest.setUpdatedAt(LocalDateTime.now());

        User currentUser = userRepository.findByUsername(username).orElse(null);
        // KN-224, KN-225, KN-226: Kiểm tra chỉ tiêu headcount nếu không phải là bản nháp
        if (!isDraft) {
            boolean isConfirmed = Boolean.TRUE.equals(dto.getIsOverHeadcountConfirmed()) || Boolean.TRUE.equals(dto.getIsOverHeadcount());
            departmentHeadcountService.validateAndProcessJobRequestHeadcount(
                    jobRequest, isConfirmed, dto.getOverHeadcountReason(), currentUser);
        }

        JobRequest saved = jobRequestRepository.save(jobRequest);

        // KN-227: Ghi audit log nếu có ghi đè headcount
        if (!isDraft && Boolean.TRUE.equals(saved.getIsOverHeadcount())) {
            departmentHeadcountService.recordInitialOverrideAudit(saved, dto.getOverHeadcountReason(), currentUser);
        }

        // KN-220, KN-221, KN-235, KN-240: Nếu chuyển sang chính thức, tạo chuỗi duyệt snapshot hoặc khôi phục bước duyệt sau khi bổ sung
        if (!isDraft) {
            List<JobRequestApprovalStep> existingSteps = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(saved.getId());
            boolean hasChangeRequestStep = existingSteps.stream()
                    .anyMatch(s -> "REQUEST_CHANGE".equalsIgnoreCase(s.getStatus()) || "CHANGES_REQUESTED".equalsIgnoreCase(s.getStatus()));

            if (hasChangeRequestStep) {
                for (JobRequestApprovalStep s : existingSteps) {
                    if ("REQUEST_CHANGE".equalsIgnoreCase(s.getStatus()) || "CHANGES_REQUESTED".equalsIgnoreCase(s.getStatus())) {
                        s.setStatus("PENDING");
                        jobRequestApprovalStepRepository.save(s);
                        saved.setCurrentApprovalStep(s.getStepOrder());
                        jobRequestRepository.save(saved);
                        break;
                    }
                }
            } else {
                approvalWorkflowService.createApprovalStepsForJobRequest(saved);
            }
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public JobRequestDto submitJobRequest(Long id, String username) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        checkModifyPermission(jobRequest, username);

        if (!"DRAFT".equalsIgnoreCase(jobRequest.getStatus()) && 
            !"REJECTED".equalsIgnoreCase(jobRequest.getStatus()) && 
            !"CHANGES_REQUESTED".equalsIgnoreCase(jobRequest.getStatus()) && 
            !"REQUEST_CHANGE".equalsIgnoreCase(jobRequest.getStatus())) {
            throw new AppException("Chỉ có thể gửi phê duyệt yêu cầu ở trạng thái Lưu nháp, Từ chối hoặc Yêu cầu bổ sung!");
        }

        JobRequestDto dto = mapToDto(jobRequest);
        validateJobRequest(dto, jobRequest.getPosition(), false);

        User currentUser = userRepository.findByUsername(username).orElse(null);
        departmentHeadcountService.validateAndProcessJobRequestHeadcount(
                jobRequest, jobRequest.getIsOverHeadcount(), jobRequest.getOverHeadcountReason(), currentUser);

        jobRequest.setStatus("PENDING_APPROVAL");
        jobRequest.setUpdatedAt(LocalDateTime.now());
        JobRequest saved = jobRequestRepository.save(jobRequest);

        // KN-227: Ghi audit log nếu có ghi đè headcount
        if (Boolean.TRUE.equals(saved.getIsOverHeadcount())) {
            departmentHeadcountService.recordInitialOverrideAudit(saved, saved.getOverHeadcountReason(), currentUser);
        }

        // KN-240: Nếu gửi lại sau khi bổ sung thông tin, giữ nguyên lịch sử và chuyển bước yêu cầu bổ sung về PENDING
        List<JobRequestApprovalStep> existingSteps = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(saved.getId());
        boolean hasChangeRequestStep = existingSteps.stream()
                .anyMatch(s -> "REQUEST_CHANGE".equalsIgnoreCase(s.getStatus()) || "CHANGES_REQUESTED".equalsIgnoreCase(s.getStatus()));

        if (hasChangeRequestStep) {
            for (JobRequestApprovalStep s : existingSteps) {
                if ("REQUEST_CHANGE".equalsIgnoreCase(s.getStatus()) || "CHANGES_REQUESTED".equalsIgnoreCase(s.getStatus())) {
                    s.setStatus("PENDING");
                    jobRequestApprovalStepRepository.save(s);
                    saved.setCurrentApprovalStep(s.getStepOrder());
                    jobRequestRepository.save(saved);
                    break;
                }
            }
        } else {
            // KN-220, KN-221, KN-235: Khởi tạo snapshot chuỗi cấp duyệt
            approvalWorkflowService.createApprovalStepsForJobRequest(saved);
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteJobRequest(Long id, String username) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        checkModifyPermission(jobRequest, username);

        if (!"DRAFT".equalsIgnoreCase(jobRequest.getStatus()) && !"REJECTED".equalsIgnoreCase(jobRequest.getStatus())) {
            throw new AppException("Chỉ có thể xóa yêu cầu ở trạng thái Lưu nháp hoặc Từ chối!");
        }

        // KN-245: Ngăn sửa và xoá lịch sử phê duyệt sau khi đã ghi nhận
        List<JobRequestApprovalStep> existingSteps = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(id);
        boolean hasActionedHistory = existingSteps.stream().anyMatch(s -> s.getApprovalDate() != null 
                || "APPROVED".equalsIgnoreCase(s.getStatus()) || "REQUEST_CHANGE".equalsIgnoreCase(s.getStatus()));
        if (hasActionedHistory) {
            throw new AppException("Không thể xóa yêu cầu tuyển dụng đã có lịch sử phê duyệt được ghi nhận (KN-245)!");
        }

        jobRequestApprovalStepRepository.deleteByJobRequestId(id);
        jobRequestRepository.delete(jobRequest);
    }

    @Override
    public JobRequestDto getJobRequestById(Long id) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));
        return mapToDto(jobRequest);
    }

    @Override
    public List<JobRequestDto> getMyJobRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));
        return jobRequestRepository.findByHiringManagerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public JobRequestDto createJobRequest(JobRequestDto dto, String username) {
        boolean draft = Boolean.TRUE.equals(dto.getIsDraft()) || "DRAFT".equalsIgnoreCase(dto.getStatus());
        return createOrDraftJobRequest(dto, username, draft);
    }

    @Override
    @Transactional
    public JobRequestDto cloneJobRequest(Long id, String username) {
        JobRequest original = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng gốc không tồn tại"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        // KN-228: Sao chép đầy đủ nội dung yêu cầu tuyển dụng sang bản sao
        String clonedTitle = original.getTitle() != null ? original.getTitle() + " (Bản sao)" : "Bản sao yêu cầu tuyển dụng";

        JobRequest clone = JobRequest.builder()
                .title(clonedTitle)
                .department(original.getDepartment())
                .position(original.getPosition())
                .location(original.getLocation())
                .employmentType(original.getEmploymentType())
                .headcount(original.getHeadcount())
                .recruitmentReason(original.getRecruitmentReason())
                .neededDate(original.getNeededDate())
                .minSalary(original.getMinSalary())
                .maxSalary(original.getMaxSalary())
                .salaryExplanation(original.getSalaryExplanation())
                .description(original.getDescription()) // Sao chép đầy đủ mô tả công việc (KN-228)
                .requirements(original.getRequirements()) // Sao chép đầy đủ yêu cầu ứng viên (KN-228)
                // KN-230: Đặt trạng thái bản sao mặc định là Nháp
                .status("DRAFT")
                .hiringManager(currentUser)
                .recruiter(null)
                // KN-229: Không sao chép lịch sử phê duyệt khi tạo bản sao
                .currentApprovalStep(null)
                .totalApprovalSteps(null)
                .approvalRuleId(null)
                .isOverHeadcount(false)
                .overHeadcountApproved(false)
                .overHeadcountReason(null)
                .overHeadcountApprovedBy(null)
                .overHeadcountApprovedAt(null)
                .approvalSteps(new java.util.ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        JobRequest saved = jobRequestRepository.save(clone);
        return mapToDto(saved);
    }

    @Override
    public List<JobRequestDto> getAllJobRequests() {
        return jobRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobRequestDto> getJobRequestsByDepartment(Long departmentId) {
        return jobRequestRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobRequestDto approveOrReject(Long id, boolean isApproved, String approverUsername) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu không tồn tại"));

        // Kiểm tra xem có chuỗi cấp duyệt snapshot (KN-235) không
        List<JobRequestApprovalStep> steps = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(id);
        if (!steps.isEmpty()) {
            Optional<JobRequestApprovalStep> pendingStep = steps.stream()
                    .filter(s -> "PENDING".equalsIgnoreCase(s.getStatus()))
                    .findFirst();

            if (pendingStep.isPresent()) {
                approvalWorkflowService.processApprovalStep(
                        id, pendingStep.get().getId(), isApproved, isApproved ? "Phê duyệt" : "Từ chối", approverUsername);
                JobRequest updated = jobRequestRepository.findById(id).orElse(jobRequest);
                return mapToDto(updated);
            }
        }

        // Quy trình đơn cấp truyền thống (fallback)
        if (isApproved) {
            jobRequest.setStatus("APPROVED");
        } else {
            jobRequest.setStatus("REJECTED");
        }
        jobRequest.setUpdatedAt(LocalDateTime.now());
        
        JobRequest updated = jobRequestRepository.save(jobRequest);
        return mapToDto(updated);
    }

    @Override
    public JobRequestDto assignRecruiter(Long id, String recruiterUsername) {
        JobRequest job = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy Yêu cầu tuyển dụng này"));
        
        User recruiter = userRepository.findByUsername(recruiterUsername)
                .orElseThrow(() -> new AppException("Không tìm thấy Recruiter này"));
                
        job.setRecruiter(recruiter);
        job.setStatus("OPEN");
        job.setUpdatedAt(LocalDateTime.now());
        JobRequest saved = jobRequestRepository.save(job);
        return mapToDto(saved);
    }

    private void checkModifyPermission(JobRequest jobRequest, String username) {
        boolean isCreator = jobRequest.getHiringManager() != null && 
                            jobRequest.getHiringManager().getUsername().equals(username);
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));
        boolean isAdmin = currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_ADMIN");

        if (!isCreator && !isAdmin) {
            throw new AppException("Bạn không có quyền thao tác trên yêu cầu tuyển dụng này!");
        }
    }

    private void validateJobRequest(JobRequestDto dto, JobPosition position, boolean isDraft) {
        if (isDraft) {
            // KN-207: Khi lưu nháp, chỉ cần có tiêu đề hoặc chức danh
            String title = dto.getTitle();
            if ((title == null || title.trim().isEmpty()) && (dto.getPositionId() == null && position == null)) {
                throw new AppException("Vui lòng nhập tiêu đề hoặc chọn chức danh để lưu nháp!");
            }
            return;
        }

        // KN-206: Khai báo chức danh, phòng ban, số lượng, lý do tuyển, dải lương đề xuất, ngày cần người
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            if (position != null) {
                dto.setTitle(position.getName());
            } else {
                throw new AppException("Vui lòng nhập tiêu đề hoặc chọn chức danh tuyển dụng!");
            }
        }

        if (dto.getDepartmentId() == null) {
            throw new AppException("Vui lòng chọn phòng ban tuyển dụng!");
        }

        if (dto.getHeadcount() == null || dto.getHeadcount() <= 0) {
            throw new AppException("Số lượng cần tuyển phải lớn hơn 0!");
        }

        if (dto.getRecruitmentReason() == null || dto.getRecruitmentReason().trim().isEmpty()) {
            throw new AppException("Vui lòng chọn lý do tuyển dụng (Thay thế hoặc Tăng mới)!");
        }

        // KN-208: Kiểm tra hợp lệ ngày cần người không được ở quá khứ
        if (dto.getNeededDate() == null) {
            throw new AppException("Vui lòng nhập ngày cần người!");
        }
        if (dto.getNeededDate().isBefore(LocalDate.now())) {
            throw new AppException("Ngày cần người không được ở quá khứ!");
        }

        // Kiểm tra dải lương đề xuất
        if (dto.getMinSalary() != null && dto.getMinSalary() < 0) {
            throw new AppException("Lương tối thiểu không được âm!");
        }
        if (dto.getMaxSalary() != null && dto.getMaxSalary() < 0) {
            throw new AppException("Lương tối đa không được âm!");
        }
        if (dto.getMinSalary() != null && dto.getMaxSalary() != null && dto.getMinSalary() > dto.getMaxSalary()) {
            throw new AppException("Lương tối thiểu không được lớn hơn lương tối đa!");
        }

        // KN-208: Dải lương đề xuất nằm ngoài dải chuẩn của chức danh thì bắt buộc nhập giải trình
        if (position != null) {
            Double standardMin = position.getMinSalary();
            Double standardMax = position.getMaxSalary();

            boolean isOutOfRange = false;
            if (standardMin != null && dto.getMinSalary() != null && dto.getMinSalary() < standardMin) {
                isOutOfRange = true;
            }
            if (standardMax != null && dto.getMaxSalary() != null && dto.getMaxSalary() > standardMax) {
                isOutOfRange = true;
            }

            if (isOutOfRange) {
                if (dto.getSalaryExplanation() == null || dto.getSalaryExplanation().trim().isEmpty()) {
                    String stdRangeStr = (standardMin != null ? String.format("%,.0f", standardMin) : "0") + " - " +
                            (standardMax != null ? String.format("%,.0f", standardMax) : "không giới hạn") + " VNĐ";
                    throw new AppException("Dải lương đề xuất nằm ngoài dải chuẩn của chức danh (" + stdRangeStr + "). Bắt buộc phải nhập giải trình lý do!");
                }
            }
        }

        // KN-207: Soạn mô tả công việc và yêu cầu ứng viên
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new AppException("Vui lòng nhập mô tả công việc!");
        }
        if (dto.getRequirements() == null || dto.getRequirements().trim().isEmpty()) {
            throw new AppException("Vui lòng nhập yêu cầu ứng viên!");
        }
    }

    private JobRequestDto mapToDto(JobRequest entity) {
        if (entity == null) return null;
        JobPosition pos = entity.getPosition();

        // Map approval steps snapshot (KN-235)
        List<JobRequestApprovalStepDto> stepDtos = null;
        if (entity.getId() != null) {
            stepDtos = jobRequestApprovalStepRepository.findByJobRequestIdOrderByStepOrderAsc(entity.getId())
                    .stream()
                    .map(s -> JobRequestApprovalStepDto.builder()
                            .id(s.getId())
                            .jobRequestId(entity.getId())
                            .stepOrder(s.getStepOrder())
                            .stepName(s.getStepName())
                            .approverRole(s.getApproverRole())
                            .approverUserId(s.getApproverUser() != null ? s.getApproverUser().getId() : null)
                            .approverUserName(s.getApproverUser() != null ? s.getApproverUser().getUsername() : null)
                            .approverUserFullName(s.getApproverUser() != null ? s.getApproverUser().getFullName() : null)
                            .status(s.getStatus())
                            .isOverBudgetStep(s.getIsOverBudgetStep())
                            .approvalDate(s.getApprovalDate())
                            .comment(s.getComment())
                            .actionByUserId(s.getActionBy() != null ? s.getActionBy().getId() : null)
                            .actionByUserName(s.getActionBy() != null ? s.getActionBy().getUsername() : null)
                            .actionByUserFullName(s.getActionBy() != null ? s.getActionBy().getFullName() : null)
                            .createdAt(s.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        // KN-244: Xác định người và cấp đang chờ phê duyệt tiếp theo
        String currentApproverName = null;
        String currentApproverRole = null;
        String currentStepName = null;

        if (stepDtos != null) {
            for (JobRequestApprovalStepDto s : stepDtos) {
                if ("PENDING".equalsIgnoreCase(s.getStatus())) {
                    currentStepName = s.getStepName();
                    currentApproverRole = s.getApproverRole();
                    if (s.getApproverUserFullName() != null && !s.getApproverUserFullName().trim().isEmpty()) {
                        currentApproverName = s.getApproverUserFullName();
                    } else if (s.getApproverUserName() != null && !s.getApproverUserName().trim().isEmpty()) {
                        currentApproverName = s.getApproverUserName();
                    } else if (s.getApproverRole() != null) {
                        currentApproverName = formatRoleDisplayName(s.getApproverRole());
                    }
                    break;
                }
            }
        }

        return JobRequestDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .headcount(entity.getHeadcount())
                .minSalary(entity.getMinSalary())
                .maxSalary(entity.getMaxSalary())
                .recruitmentReason(entity.getRecruitmentReason())
                .neededDate(entity.getNeededDate())
                .salaryExplanation(entity.getSalaryExplanation())
                .description(entity.getDescription())
                .requirements(entity.getRequirements())
                .status(entity.getStatus())
                .isDraft("DRAFT".equalsIgnoreCase(entity.getStatus()))
                .currentApprovalStep(entity.getCurrentApprovalStep())
                .totalApprovalSteps(entity.getTotalApprovalSteps())
                .approvalRuleId(entity.getApprovalRuleId())
                .approvalSteps(stepDtos)
                .currentApproverName(currentApproverName)
                .currentApproverRole(currentApproverRole)
                .currentStepName(currentStepName)
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
                .positionId(pos != null ? pos.getId() : null)
                .positionName(pos != null ? pos.getName() : null)
                .positionCode(pos != null ? pos.getCode() : null)
                .positionMinSalary(pos != null ? pos.getMinSalary() : null)
                .positionMaxSalary(pos != null ? pos.getMaxSalary() : null)
                .hiringManagerId(entity.getHiringManager() != null ? entity.getHiringManager().getId() : null)
                .hiringManagerName(entity.getHiringManager() != null ? 
                        (entity.getHiringManager().getFullName() != null && !entity.getHiringManager().getFullName().isEmpty() 
                                ? entity.getHiringManager().getFullName() 
                                : entity.getHiringManager().getUsername()) : null)
                .recruiterId(entity.getRecruiter() != null ? entity.getRecruiter().getId() : null)
                .recruiterName(entity.getRecruiter() != null ? 
                        (entity.getRecruiter().getFullName() != null && !entity.getRecruiter().getFullName().isEmpty() 
                                ? entity.getRecruiter().getFullName() 
                                : entity.getRecruiter().getUsername()) : null)
                .isOverHeadcount(entity.getIsOverHeadcount())
                .overHeadcountApproved(entity.getOverHeadcountApproved())
                .overHeadcountReason(entity.getOverHeadcountReason())
                .overHeadcountApprovedBy(entity.getOverHeadcountApprovedBy())
                .overHeadcountApprovedAt(entity.getOverHeadcountApprovedAt())
                .closingReason(entity.getClosingReason())
                .closingNote(entity.getClosingNote())
                .closedAt(entity.getClosedAt())
                .closedBy(entity.getClosedBy())
                .activeApplicationsCount(getActiveApplicationsCount(entity.getId()))
                .locationId(entity.getLocation() != null ? entity.getLocation().getId() : null)
                .locationName(entity.getLocation() != null ? entity.getLocation().getName() : null)
                .employmentTypeId(entity.getEmploymentType() != null ? entity.getEmploymentType().getId() : null)
                .employmentTypeName(entity.getEmploymentType() != null ? entity.getEmploymentType().getName() : null)
                .applicationDeadline(entity.getApplicationDeadline())
                .isSalaryVisible(entity.getIsSalaryVisible() != null ? entity.getIsSalaryVisible() : true)
                .publishedAt(entity.getPublishedAt())
                .publishedBy(entity.getPublishedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Integer getActiveApplicationsCount(Long jobRequestId) {
        if (jobRequestId == null) return 0;
        try {
            List<Application> apps = applicationRepository.findByJobRequestId(jobRequestId);
            if (apps == null) return 0;
            return (int) apps.stream()
                    .filter(a -> a.getStatus() != null && 
                            !"HIRED".equalsIgnoreCase(a.getStatus()) && 
                            !"REJECTED".equalsIgnoreCase(a.getStatus()) && 
                            !"WITHDRAWN".equalsIgnoreCase(a.getStatus()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public PipelineCheckDto checkCandidatePipeline(Long jobRequestId) {
        JobRequest job = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        List<Application> allApps = applicationRepository.findByJobRequestId(jobRequestId);
        if (allApps == null) allApps = List.of();

        List<ApplicationSummaryDto> activeApps = allApps.stream()
                .filter(a -> a.getStatus() != null && 
                        !"HIRED".equalsIgnoreCase(a.getStatus()) && 
                        !"REJECTED".equalsIgnoreCase(a.getStatus()) && 
                        !"WITHDRAWN".equalsIgnoreCase(a.getStatus()))
                .map(a -> {
                    String name = "Ứng viên";
                    String email = "";
                    String phone = "";
                    if (a.getCandidateProfile() != null) {
                        if (a.getCandidateProfile().getFullName() != null && !a.getCandidateProfile().getFullName().trim().isEmpty()) {
                            name = a.getCandidateProfile().getFullName();
                        } else if (a.getCandidateProfile().getUser() != null) {
                            name = a.getCandidateProfile().getUser().getFullName() != null ? 
                                    a.getCandidateProfile().getUser().getFullName() : a.getCandidateProfile().getUser().getUsername();
                        }
                        phone = a.getCandidateProfile().getPhone() != null ? a.getCandidateProfile().getPhone() : "";
                        if (a.getCandidateProfile().getUser() != null) {
                            email = a.getCandidateProfile().getUser().getEmail() != null ? a.getCandidateProfile().getUser().getEmail() : "";
                        }
                    }
                    return ApplicationSummaryDto.builder()
                            .id(a.getId())
                            .candidateName(name)
                            .candidateEmail(email)
                            .candidatePhone(phone)
                            .status(a.getStatus())
                            .appliedAt(a.getAppliedAt())
                            .build();
                })
                .collect(Collectors.toList());

        boolean hasActive = !activeApps.isEmpty();
        String warning = hasActive ? 
                "Cảnh báo: Yêu cầu tuyển dụng này còn " + activeApps.size() + " ứng viên đang trong pipeline xử lý chưa có kết quả cuối cùng (KN-248). Bạn cần xử lý ứng viên hoặc xác nhận để đóng yêu cầu (KN-249)!" :
                "Tất cả ứng viên đã được xử lý xong hoặc chưa có hồ sơ ứng tuyển nào.";

        return PipelineCheckDto.builder()
                .jobRequestId(job.getId())
                .jobTitle(job.getTitle())
                .status(job.getStatus())
                .totalApplications(allApps.size())
                .activeApplicationsCount(activeApps.size())
                .hasActiveCandidates(hasActive)
                .warningMessage(warning)
                .activeApplications(activeApps)
                .build();
    }

    @Override
    @Transactional
    public JobRequestDto closeJobRequest(Long jobRequestId, CloseJobRequestDto dto, String username) {
        JobRequest job = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        // Quyền đóng: HR_MANAGER, ADMIN hoặc người tạo
        boolean isHr = currentUser.getRoles() != null && 
                (currentUser.getRoles().contains("ROLE_HR_MANAGER") || currentUser.getRoles().contains("ROLE_ADMIN"));
        boolean isCreator = job.getHiringManager() != null && job.getHiringManager().getUsername().equals(username);
        if (!isHr && !isCreator) {
            throw new AppException("Bạn không có quyền đóng hoặc kết thúc yêu cầu tuyển dụng này!");
        }

        if (dto.getClosingReason() == null || dto.getClosingReason().trim().isEmpty()) {
            throw new AppException("Vui lòng chọn 1 trong 3 trạng thái kết thúc (Đã tuyển đủ, Tạm dừng, Huỷ)!");
        }

        // Chuẩn hoá lý do kết thúc (KN-246)
        String rawReason = dto.getClosingReason().trim().toUpperCase();
        String normalizedReason;
        String targetStatus;
        if (rawReason.contains("FILL") || rawReason.contains("ĐỦ") || rawReason.contains("DU")) {
            normalizedReason = "FILLED";
            targetStatus = "CLOSED";
        } else if (rawReason.contains("PAUS") || rawReason.contains("DỪNG") || rawReason.contains("DUNG")) {
            normalizedReason = "PAUSED";
            targetStatus = "PAUSED";
        } else if (rawReason.contains("CANCEL") || rawReason.contains("HUỶ") || rawReason.contains("HUY")) {
            normalizedReason = "CANCELLED";
            targetStatus = "CANCELLED";
        } else {
            throw new AppException("Trạng thái kết thúc không hợp lệ. Chỉ chấp nhận: FILLED (Đã tuyển đủ), PAUSED (Tạm dừng), CANCELLED (Huỷ)!");
        }

        // KN-248, KN-249: Kiểm tra ứng viên trong pipeline
        List<Application> allApps = applicationRepository.findByJobRequestId(jobRequestId);
        if (allApps == null) allApps = List.of();
        long activeCount = allApps.stream()
                .filter(a -> a.getStatus() != null && 
                        !"HIRED".equalsIgnoreCase(a.getStatus()) && 
                        !"REJECTED".equalsIgnoreCase(a.getStatus()) && 
                        !"WITHDRAWN".equalsIgnoreCase(a.getStatus()))
                .count();

        if (activeCount > 0 && !Boolean.TRUE.equals(dto.getConfirmCloseWithPendingCandidates())) {
            throw new AppException("Không thể đóng yêu cầu tuyển dụng vì còn " + activeCount + 
                    " ứng viên đang trong pipeline xử lý (KN-249). Vui lòng xử lý ứng viên hoặc xác nhận đồng ý đóng!");
        }

        job.setStatus(targetStatus);
        job.setClosingReason(normalizedReason);
        job.setClosingNote(dto.getClosingNote());
        job.setClosedAt(LocalDateTime.now());
        job.setClosedBy(currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty() ? 
                currentUser.getFullName() : currentUser.getUsername());
        job.setUpdatedAt(LocalDateTime.now());

        JobRequest saved = jobRequestRepository.save(job);
        return mapToDto(saved);
    }

    private String formatRoleDisplayName(String role) {
        if (role == null) return null;
        switch (role) {
            case "ROLE_ADMIN":
                return "Ban Giám đốc / Admin";
            case "ROLE_HR_MANAGER":
                return "Trưởng phòng Nhân sự";
            case "ROLE_HIRING_MANAGER":
                return "Trưởng bộ phận";
            case "ROLE_APPROVER":
                return "Người duyệt";
            case "ROLE_RECRUITER":
                return "Chuyên viên tuyển dụng";
            default:
                return role.replace("ROLE_", "");
        }
    }

    @Override
    public List<Map<String, Object>> getAllRecruiters() {
        return userRepository.findAllActiveRecruiters().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("fullName", (u.getFullName() != null && !u.getFullName().trim().isEmpty()) ? u.getFullName() : u.getUsername());
            map.put("email", u.getEmail());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Đăng tin & Cổng ứng tuyển - Nhân viên tuyển dụng:
     * - Chỉ yêu cầu ở trạng thái Đã duyệt mới tạo được tin.
     * - Nội dung mô tả công việc được kế thừa từ yêu cầu, sửa lại được cho phù hợp ngôn ngữ tuyển dụng.
     * - Khai báo địa điểm, hình thức làm việc, hạn nhận hồ sơ, có hiển thị mức lương hay không.
     */
    @Override
    @Transactional
    public JobRequestDto publishJobPosting(Long id, com.example.auth.dto.PublishJobPostingDto dto, String username) {
        JobRequest job = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        // 1. Chỉ yêu cầu ở trạng thái Đã duyệt mới tạo được tin (hoặc OPEN nếu cập nhật tin đã đăng)
        if (!"APPROVED".equalsIgnoreCase(job.getStatus()) && !"OPEN".equalsIgnoreCase(job.getStatus())) {
            throw new AppException("Chỉ yêu cầu tuyển dụng ở trạng thái Đã duyệt mới tạo được tin tuyển dụng! Trạng thái hiện tại: " + job.getStatus());
        }

        // 2. Kiểm tra tiêu đề và hạn nhận hồ sơ (KN-256)
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new AppException("Tiêu đề tin tuyển dụng không được để trống!");
        }

        if (dto.getApplicationDeadline() != null && dto.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new AppException("Hạn nhận hồ sơ không được ở trong quá khứ! Phải là ngày hôm nay hoặc trong tương lai.");
        }

        // 3. Khai báo địa điểm làm việc
        RecruitmentCategory location = null;
        if (dto.getLocationId() != null) {
            location = recruitmentCategoryRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new AppException("Không tìm thấy địa điểm làm việc đã chọn"));
        }

        // 4. Khai báo hình thức làm việc
        RecruitmentCategory employmentType = null;
        if (dto.getEmploymentTypeId() != null) {
            employmentType = recruitmentCategoryRepository.findById(dto.getEmploymentTypeId())
                    .orElseThrow(() -> new AppException("Không tìm thấy hình thức làm việc đã chọn"));
        }

        // 5. Nội dung mô tả và yêu cầu kế thừa từ yêu cầu, được Recruiter sửa lại cho phù hợp ngôn ngữ tuyển dụng (KN-254)
        job.setTitle(dto.getTitle().trim());
        if (dto.getDescription() != null) {
            job.setDescription(dto.getDescription().trim());
        }
        if (dto.getRequirements() != null) {
            job.setRequirements(dto.getRequirements().trim());
        }
        job.setLocation(location);
        job.setEmploymentType(employmentType);
        job.setApplicationDeadline(dto.getApplicationDeadline());
        job.setIsSalaryVisible(dto.getIsSalaryVisible() != null ? dto.getIsSalaryVisible() : true);

        // Xuất bản tin: Chuyển trạng thái sang OPEN (Đang mở tuyển / Đã xuất bản) (KN-258)
        job.setStatus("OPEN");
        job.setPublishedAt(LocalDateTime.now());

        User currentUser = userRepository.findByUsername(username).orElse(null);
        String publisherName = (currentUser != null && currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty())
                ? currentUser.getFullName() : username;
        job.setPublishedBy(publisherName);

        // Nếu chưa có recruiter phụ trách, tự động gán cho người đăng tin
        if (job.getRecruiter() == null && currentUser != null) {
            job.setRecruiter(currentUser);
        }

        job.setUpdatedAt(LocalDateTime.now());
        JobRequest saved = jobRequestRepository.save(job);
        return mapToDto(saved);
    }

    /**
     * KN-301: Cho phép nhà tuyển dụng gỡ tin thủ công bất cứ lúc nào
     * Khi gỡ tin, tin biến mất khỏi trang việc làm công khai ngay lập tức.
     */
    @Override
    @Transactional
    public JobRequestDto unpublishJobPosting(Long id, String username) {
        JobRequest job = jobRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại"));

        if (job.getPublishedAt() == null && !"OPEN".equalsIgnoreCase(job.getStatus())) {
            throw new AppException("Tin tuyển dụng này hiện chưa được xuất bản hoặc đã được gỡ trước đó!");
        }

        // Gỡ tin công khai: xóa mốc thời gian xuất bản và đưa về trạng thái đã duyệt (APPROVED)
        job.setPublishedAt(null);
        job.setPublishedBy(null);
        job.setStatus("APPROVED");
        job.setUpdatedAt(LocalDateTime.now());

        JobRequest saved = jobRequestRepository.save(job);
        log.info("Nhà tuyển dụng '{}' đã gỡ tin tuyển dụng ID={} ({})", username, saved.getId(), saved.getTitle());
        return mapToDto(saved);
    }

    /**
     * KN-302, KN-303: Tác vụ tự động quét và ẩn tin quá hạn nhận hồ sơ
     * Điều kiện: applicationDeadline < LocalDate.now() và tin đang mở/đang xuất bản
     */
    @Override
    @Transactional
    public int autoExpireJobs() {
        LocalDate today = LocalDate.now();
        List<JobRequest> all = jobRequestRepository.findAll();
        int count = 0;

        for (JobRequest job : all) {
            boolean isPublishedOrOpen = "OPEN".equalsIgnoreCase(job.getStatus()) || job.getPublishedAt() != null;
            boolean isExpired = job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(today);

            if (isPublishedOrOpen && isExpired) {
                log.info(">>> [AUTO EXPIRE] Ẩn tin tuyển dụng quá hạn: ID={}, Tiêu đề={}, Hạn chót={}",
                        job.getId(), job.getTitle(), job.getApplicationDeadline());

                job.setStatus("CLOSED");
                job.setClosingReason("EXPIRED");
                job.setClosingNote("Hệ thống tự động đóng và ẩn tin do đã quá hạn nhận hồ sơ ngày " + job.getApplicationDeadline());
                job.setClosedAt(LocalDateTime.now());
                job.setClosedBy("HỆ THỐNG");
                job.setUpdatedAt(LocalDateTime.now());

                jobRequestRepository.save(job);
                count++;
            }
        }

        log.info(">>> [AUTO EXPIRE] Hoàn tất quét tin hết hạn. Tổng số tin đã tự động ẩn: {}", count);
        return count;
    }
}

package com.example.auth.service.impl;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.DepartmentHeadcountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepartmentHeadcountServiceImpl implements DepartmentHeadcountService {

    private final DepartmentHeadcountPlanRepository planRepository;
    private final HeadcountOverrideAuditRepository auditRepository;
    private final DepartmentRepository departmentRepository;
    private final JobRequestRepository jobRequestRepository;
    private final UserRepository userRepository;

    @Override
    public DepartmentHeadcountPlanDto createOrUpdatePlan(DepartmentHeadcountPlanDto dto, String currentUsername) {
        if (dto.getDepartmentId() == null) {
            throw new AppException("Vui lòng chọn phòng ban để khai báo chỉ tiêu!");
        }
        if (dto.getYear() == null || dto.getYear() < 2000 || dto.getYear() > 2100) {
            throw new AppException("Năm kế hoạch không hợp lệ (từ 2000 đến 2100)!");
        }
        if (dto.getTargetHeadcount() == null || dto.getTargetHeadcount() < 0) {
            throw new AppException("Chỉ tiêu headcount không được để trống hoặc âm!");
        }
        if (dto.getSalaryBudget() == null || dto.getSalaryBudget().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException("Ngân sách lương không được để trống hoặc âm!");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new AppException("Phòng ban không tồn tại!"));

        DepartmentHeadcountPlan plan;
        if (dto.getId() != null) {
            plan = planRepository.findById(dto.getId())
                    .orElseThrow(() -> new AppException("Kế hoạch headcount không tồn tại!"));
            if (planRepository.existsByDepartmentIdAndYearAndIdNot(dto.getDepartmentId(), dto.getYear(), dto.getId())) {
                throw new AppException("Phòng ban " + department.getName() + " đã có chỉ tiêu cho năm " + dto.getYear() + "!");
            }
        } else {
            Optional<DepartmentHeadcountPlan> existing = planRepository.findByDepartmentIdAndYear(dto.getDepartmentId(), dto.getYear());
            if (existing.isPresent()) {
                // Tự động chuyển sang cập nhật bản ghi đã có
                plan = existing.get();
            } else {
                plan = new DepartmentHeadcountPlan();
                plan.setCreatedBy(currentUsername);
            }
        }

        plan.setDepartment(department);
        plan.setYear(dto.getYear());
        plan.setTargetHeadcount(dto.getTargetHeadcount());
        plan.setSalaryBudget(dto.getSalaryBudget());
        plan.setNote(dto.getNote());
        plan.setUpdatedBy(currentUsername);

        DepartmentHeadcountPlan saved = planRepository.save(plan);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentHeadcountPlanDto getPlanByDepartmentAndYear(Long departmentId, Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        DepartmentHeadcountPlan plan = planRepository.findByDepartmentIdAndYear(departmentId, year)
                .orElse(null);
        if (plan == null) return null;
        return mapToDto(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentHeadcountPlanDto> getPlansByYear(Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        return planRepository.findByYearOrderByDepartmentNameAsc(year).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentHeadcountPlanDto> getAllPlans() {
        return planRepository.findAllByOrderByYearDescDepartmentNameAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deletePlan(Long id) {
        DepartmentHeadcountPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException("Chỉ tiêu không tồn tại!"));
        planRepository.delete(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public HeadcountCheckResponseDto checkDepartmentHeadcount(Long departmentId, Integer year, Integer requestedHeadcount, Long excludeJobRequestId) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException("Phòng ban không tồn tại!"));

        Optional<DepartmentHeadcountPlan> planOpt = planRepository.findByDepartmentIdAndYear(departmentId, year);
        boolean hasPlan = planOpt.isPresent();
        int targetHeadcount = hasPlan ? planOpt.get().getTargetHeadcount() : 0;
        BigDecimal salaryBudget = hasPlan && planOpt.get().getSalaryBudget() != null ? planOpt.get().getSalaryBudget() : BigDecimal.ZERO;

        // Tính used headcount của phòng ban trong năm
        int usedHeadcount = calculateUsedHeadcount(departmentId, year, excludeJobRequestId);
        int remainingHeadcount = targetHeadcount - usedHeadcount;

        int reqCount = requestedHeadcount != null ? requestedHeadcount : 0;
        boolean willExceed = hasPlan && ((usedHeadcount + reqCount) > targetHeadcount);
        int exceededCount = willExceed ? (usedHeadcount + reqCount - targetHeadcount) : 0;

        String warningMessage = null;
        if (willExceed) {
            warningMessage = String.format(
                    "CẢNH BÁO VƯỢT CHỈ TIÊU (KN-225): Phòng ban %s trong năm %d có chỉ tiêu %d nhân sự, đã đăng ký %d, còn lại %d chỉ tiêu. Yêu cầu tuyển %d nhân sự làm vượt %d chỉ tiêu! Cần Trưởng phòng Nhân sự xác nhận ghi đè kèm lý do.",
                    department.getName(), year, targetHeadcount, usedHeadcount, remainingHeadcount, reqCount, exceededCount
            );
        }

        return HeadcountCheckResponseDto.builder()
                .departmentId(departmentId)
                .departmentName(department.getName())
                .year(year)
                .hasPlan(hasPlan)
                .targetHeadcount(targetHeadcount)
                .usedHeadcount(usedHeadcount)
                .remainingHeadcount(remainingHeadcount)
                .salaryBudget(salaryBudget)
                .requestedHeadcount(reqCount)
                .willExceed(willExceed)
                .exceededCount(exceededCount)
                .warningMessage(warningMessage)
                .build();
    }

    @Override
    public void validateAndProcessJobRequestHeadcount(JobRequest jobRequest, Boolean isOverrideConfirmed, String overrideReason, User currentUser) {
        if (jobRequest.getDepartment() == null) return;

        int year = jobRequest.getNeededDate() != null
                ? jobRequest.getNeededDate().getYear()
                : (jobRequest.getCreatedAt() != null ? jobRequest.getCreatedAt().getYear() : LocalDate.now().getYear());

        int requestedHeadcount = jobRequest.getHeadcount() != null ? jobRequest.getHeadcount() : 1;
        Long excludeId = jobRequest.getId();

        HeadcountCheckResponseDto check = checkDepartmentHeadcount(jobRequest.getDepartment().getId(), year, requestedHeadcount, excludeId);

        if (Boolean.TRUE.equals(check.getWillExceed())) {
            // KN-225: Vượt chỉ tiêu mà chưa có xác nhận ghi đè kèm lý do -> CHẶN LƯU
            if (!Boolean.TRUE.equals(isOverrideConfirmed) || overrideReason == null || overrideReason.trim().isEmpty()) {
                throw new AppException(check.getWarningMessage());
            }

            // KN-226: Có xác nhận ghi đè kèm lý do
            boolean isHrManagerOrAdmin = currentUser != null && currentUser.getRoles() != null &&
                    (currentUser.getRoles().contains("ROLE_HR_MANAGER") || currentUser.getRoles().contains("ROLE_ADMIN"));

            jobRequest.setIsOverHeadcount(true);
            jobRequest.setOverHeadcountReason(overrideReason.trim());

            if (isHrManagerOrAdmin) {
                // Trưởng phòng Nhân sự / Admin trực tiếp tạo hoặc duyệt ghi đè
                jobRequest.setOverHeadcountApproved(true);
                jobRequest.setOverHeadcountApprovedBy(currentUser.getUsername());
                jobRequest.setOverHeadcountApprovedAt(LocalDateTime.now());

                // KN-227: Ghi audit log nếu entity đã có ID
                if (jobRequest.getId() != null) {
                    saveAuditLog(jobRequest, check, "DIRECT_OVERRIDE", overrideReason.trim(), currentUser);
                }
            } else {
                // Trưởng bộ phận gửi đề xuất xin vượt chỉ tiêu
                jobRequest.setOverHeadcountApproved(false);

                // KN-227: Ghi audit log nếu entity đã có ID
                if (jobRequest.getId() != null) {
                    saveAuditLog(jobRequest, check, "REQUEST_OVERRIDE", overrideReason.trim(), currentUser);
                }
            }
        } else {
            // Không vượt chỉ tiêu
            jobRequest.setIsOverHeadcount(false);
            jobRequest.setOverHeadcountApproved(false);
            jobRequest.setOverHeadcountReason(null);
            jobRequest.setOverHeadcountApprovedBy(null);
            jobRequest.setOverHeadcountApprovedAt(null);
        }
    }

    @Override
    public JobRequestDto processHeadcountOverrideAction(HeadcountOverrideActionRequest request, String currentUsername) {
        if (request.getJobRequestId() == null) {
            throw new AppException("Vui lòng cung cấp ID yêu cầu tuyển dụng!");
        }

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại!"));

        boolean isHrManagerOrAdmin = currentUser.getRoles() != null &&
                (currentUser.getRoles().contains("ROLE_HR_MANAGER") || currentUser.getRoles().contains("ROLE_ADMIN"));

        if (!isHrManagerOrAdmin) {
            throw new AppException("Chỉ Trưởng phòng Nhân sự hoặc Quản trị hệ thống mới có quyền xác nhận ghi đè chỉ tiêu headcount!");
        }

        JobRequest jobRequest = jobRequestRepository.findById(request.getJobRequestId())
                .orElseThrow(() -> new AppException("Yêu cầu tuyển dụng không tồn tại!"));

        int year = jobRequest.getNeededDate() != null ? jobRequest.getNeededDate().getYear() : LocalDate.now().getYear();
        HeadcountCheckResponseDto check = checkDepartmentHeadcount(
                jobRequest.getDepartment().getId(), year, jobRequest.getHeadcount(), jobRequest.getId());

        String reason = request.getReason() != null ? request.getReason().trim() : "";

        if (Boolean.TRUE.equals(request.getApproved())) {
            jobRequest.setOverHeadcountApproved(true);
            jobRequest.setOverHeadcountApprovedBy(currentUser.getUsername());
            jobRequest.setOverHeadcountApprovedAt(LocalDateTime.now());
            if (!reason.isEmpty()) {
                jobRequest.setOverHeadcountReason(
                        (jobRequest.getOverHeadcountReason() != null ? jobRequest.getOverHeadcountReason() + " | " : "") +
                                "Phê duyệt ghi đè: " + reason
                );
            }
            saveAuditLog(jobRequest, check, "APPROVE_OVERRIDE", reason.isEmpty() ? "Trưởng phòng Nhân sự duyệt ghi đè" : reason, currentUser);
        } else {
            jobRequest.setOverHeadcountApproved(false);
            jobRequest.setStatus("REJECTED");
            if (!reason.isEmpty()) {
                jobRequest.setOverHeadcountReason(
                        (jobRequest.getOverHeadcountReason() != null ? jobRequest.getOverHeadcountReason() + " | " : "") +
                                "Từ chối ghi đè: " + reason
                );
            }
            saveAuditLog(jobRequest, check, "REJECT_OVERRIDE", reason.isEmpty() ? "Trưởng phòng Nhân sự từ chối ghi đè" : reason, currentUser);
        }

        jobRequest.setUpdatedAt(LocalDateTime.now());
        JobRequest saved = jobRequestRepository.save(jobRequest);

        return mapJobRequestToDto(saved);
    }

    @Override
    public void recordInitialOverrideAudit(JobRequest jobRequest, String reason, User currentUser) {
        if (jobRequest == null || !Boolean.TRUE.equals(jobRequest.getIsOverHeadcount())) return;
        int year = jobRequest.getNeededDate() != null
                ? jobRequest.getNeededDate().getYear()
                : (jobRequest.getCreatedAt() != null ? jobRequest.getCreatedAt().getYear() : LocalDate.now().getYear());
        Long deptId = jobRequest.getDepartment() != null ? jobRequest.getDepartment().getId() : null;
        if (deptId == null) return;

        HeadcountCheckResponseDto check = checkDepartmentHeadcount(deptId, year, jobRequest.getHeadcount(), jobRequest.getId());

        boolean isHrManagerOrAdmin = currentUser != null && currentUser.getRoles() != null &&
                (currentUser.getRoles().contains("ROLE_HR_MANAGER") || currentUser.getRoles().contains("ROLE_ADMIN"));
        String actionType = isHrManagerOrAdmin ? "DIRECT_OVERRIDE" : "REQUEST_OVERRIDE";

        saveAuditLog(jobRequest, check, actionType,
                reason != null && !reason.trim().isEmpty() ? reason.trim() : (jobRequest.getOverHeadcountReason() != null ? jobRequest.getOverHeadcountReason() : ""),
                currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeadcountOverrideAuditDto> getOverrideAudits(Long jobRequestId, Long departmentId, Integer year) {
        List<HeadcountOverrideAudit> list;
        if (jobRequestId != null) {
            list = auditRepository.findByJobRequestIdOrderByCreatedAtDesc(jobRequestId);
        } else if (departmentId != null) {
            list = auditRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
        } else if (year != null) {
            list = auditRepository.findByYearOrderByCreatedAtDesc(year);
        } else {
            list = auditRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(this::mapAuditToDto).collect(Collectors.toList());
    }

    private void saveAuditLog(JobRequest jobRequest, HeadcountCheckResponseDto check, String actionType, String reason, User currentUser) {
        int year = jobRequest.getNeededDate() != null ? jobRequest.getNeededDate().getYear() : LocalDate.now().getYear();

        HeadcountOverrideAudit audit = HeadcountOverrideAudit.builder()
                .jobRequestId(jobRequest.getId() != null ? jobRequest.getId() : 0L)
                .jobRequestTitle(jobRequest.getTitle())
                .departmentId(jobRequest.getDepartment() != null ? jobRequest.getDepartment().getId() : check.getDepartmentId())
                .departmentName(jobRequest.getDepartment() != null ? jobRequest.getDepartment().getName() : check.getDepartmentName())
                .year(year)
                .targetHeadcount(check.getTargetHeadcount())
                .usedHeadcountBefore(check.getUsedHeadcount())
                .requestedHeadcount(jobRequest.getHeadcount())
                .exceededHeadcount(check.getExceededCount())
                .actionType(actionType)
                .reason(reason)
                .actionBy(currentUser != null ? currentUser.getUsername() : "System")
                .actionByRole(currentUser != null && currentUser.getRoles() != null ? String.join(", ", currentUser.getRoles()) : "UNKNOWN")
                .createdAt(LocalDateTime.now())
                .build();

        auditRepository.save(audit);
    }

    private int calculateUsedHeadcount(Long departmentId, int year, Long excludeJobRequestId) {
        List<JobRequest> requests = jobRequestRepository.findByDepartmentId(departmentId);
        int total = 0;
        for (JobRequest req : requests) {
            if (excludeJobRequestId != null && excludeJobRequestId.equals(req.getId())) {
                continue;
            }
            // Không tính các yêu cầu bị từ chối hoặc hủy
            String status = req.getStatus() != null ? req.getStatus().toUpperCase() : "";
            if ("REJECTED".equals(status) || "CANCELLED".equals(status)) {
                continue;
            }

            int reqYear = req.getNeededDate() != null
                    ? req.getNeededDate().getYear()
                    : (req.getCreatedAt() != null ? req.getCreatedAt().getYear() : LocalDate.now().getYear());

            if (reqYear == year) {
                total += (req.getHeadcount() != null ? req.getHeadcount() : 0);
            }
        }
        return total;
    }

    private BigDecimal calculateCurrentSalaryUsed(Long departmentId, int year) {
        List<JobRequest> requests = jobRequestRepository.findByDepartmentId(departmentId);
        BigDecimal total = BigDecimal.ZERO;
        for (JobRequest req : requests) {
            String status = req.getStatus() != null ? req.getStatus().toUpperCase() : "";
            if ("REJECTED".equals(status) || "CANCELLED".equals(status)) {
                continue;
            }
            int reqYear = req.getNeededDate() != null
                    ? req.getNeededDate().getYear()
                    : (req.getCreatedAt() != null ? req.getCreatedAt().getYear() : LocalDate.now().getYear());
            if (reqYear == year) {
                int count = req.getHeadcount() != null ? req.getHeadcount() : 1;
                Double sal = req.getMaxSalary() != null ? req.getMaxSalary() : (req.getMinSalary() != null ? req.getMinSalary() : 0.0);
                total = total.add(BigDecimal.valueOf(sal * count));
            }
        }
        return total;
    }

    private DepartmentHeadcountPlanDto mapToDto(DepartmentHeadcountPlan plan) {
        int usedHeadcount = calculateUsedHeadcount(plan.getDepartment().getId(), plan.getYear(), null);
        int remainingHeadcount = plan.getTargetHeadcount() - usedHeadcount;
        BigDecimal currentSalaryUsed = calculateCurrentSalaryUsed(plan.getDepartment().getId(), plan.getYear());
        BigDecimal remainingBudget = plan.getSalaryBudget() != null
                ? plan.getSalaryBudget().subtract(currentSalaryUsed)
                : BigDecimal.ZERO;

        return DepartmentHeadcountPlanDto.builder()
                .id(plan.getId())
                .departmentId(plan.getDepartment().getId())
                .departmentName(plan.getDepartment().getName())
                .departmentCode(plan.getDepartment().getCode())
                .year(plan.getYear())
                .targetHeadcount(plan.getTargetHeadcount())
                .salaryBudget(plan.getSalaryBudget())
                .note(plan.getNote())
                .usedHeadcount(usedHeadcount)
                .remainingHeadcount(remainingHeadcount)
                .currentSalaryUsed(currentSalaryUsed)
                .remainingSalaryBudget(remainingBudget)
                .createdBy(plan.getCreatedBy())
                .updatedBy(plan.getUpdatedBy())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private HeadcountOverrideAuditDto mapAuditToDto(HeadcountOverrideAudit audit) {
        String typeName = audit.getActionType();
        switch (audit.getActionType()) {
            case "DIRECT_OVERRIDE": typeName = "HR Manager ghi đè trực tiếp"; break;
            case "REQUEST_OVERRIDE": typeName = "Đề xuất vượt chỉ tiêu"; break;
            case "APPROVE_OVERRIDE": typeName = "Chấp thuận ghi đè"; break;
            case "REJECT_OVERRIDE": typeName = "Từ chối ghi đè"; break;
        }

        return HeadcountOverrideAuditDto.builder()
                .id(audit.getId())
                .jobRequestId(audit.getJobRequestId())
                .jobRequestTitle(audit.getJobRequestTitle())
                .departmentId(audit.getDepartmentId())
                .departmentName(audit.getDepartmentName())
                .year(audit.getYear())
                .targetHeadcount(audit.getTargetHeadcount())
                .usedHeadcountBefore(audit.getUsedHeadcountBefore())
                .requestedHeadcount(audit.getRequestedHeadcount())
                .exceededHeadcount(audit.getExceededHeadcount())
                .actionType(audit.getActionType())
                .actionTypeName(typeName)
                .reason(audit.getReason())
                .actionBy(audit.getActionBy())
                .actionByRole(audit.getActionByRole())
                .createdAt(audit.getCreatedAt())
                .build();
    }

    private JobRequestDto mapJobRequestToDto(JobRequest r) {
        return JobRequestDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .headcount(r.getHeadcount())
                .minSalary(r.getMinSalary())
                .maxSalary(r.getMaxSalary())
                .description(r.getDescription())
                .requirements(r.getRequirements())
                .status(r.getStatus())
                .departmentId(r.getDepartment() != null ? r.getDepartment().getId() : null)
                .departmentName(r.getDepartment() != null ? r.getDepartment().getName() : null)
                .positionId(r.getPosition() != null ? r.getPosition().getId() : null)
                .positionName(r.getPosition() != null ? r.getPosition().getName() : null)
                .positionCode(r.getPosition() != null ? r.getPosition().getCode() : null)
                .recruitmentReason(r.getRecruitmentReason())
                .neededDate(r.getNeededDate())
                .salaryExplanation(r.getSalaryExplanation())
                .currentApprovalStep(r.getCurrentApprovalStep())
                .totalApprovalSteps(r.getTotalApprovalSteps())
                .approvalRuleId(r.getApprovalRuleId())
                .hiringManagerId(r.getHiringManager() != null ? r.getHiringManager().getId() : null)
                .hiringManagerName(r.getHiringManager() != null ? (r.getHiringManager().getFullName() != null ? r.getHiringManager().getFullName() : r.getHiringManager().getUsername()) : null)
                .isOverHeadcount(r.getIsOverHeadcount())
                .overHeadcountApproved(r.getOverHeadcountApproved())
                .overHeadcountReason(r.getOverHeadcountReason())
                .overHeadcountApprovedBy(r.getOverHeadcountApprovedBy())
                .overHeadcountApprovedAt(r.getOverHeadcountApprovedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}

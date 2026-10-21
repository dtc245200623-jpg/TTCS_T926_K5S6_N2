package com.example.auth.service.impl;

import com.example.auth.dto.JobPositionRequest;
import com.example.auth.dto.JobPositionResponse;
import com.example.auth.entity.Department;
import com.example.auth.entity.JobPosition;
import com.example.auth.exception.AppException;
import com.example.auth.repository.DepartmentRepository;
import com.example.auth.repository.JobPositionRepository;
import com.example.auth.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.example.auth.entity.CompetencyFramework;
import com.example.auth.repository.CompetencyFrameworkRepository;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPositionServiceImpl implements JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final DepartmentRepository departmentRepository;
    private final CompetencyFrameworkRepository competencyFrameworkRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobPositionResponse> getAllPositions(Boolean activeOnly, String search) {
        boolean canViewSalary = canCurrentUserViewSalary();
        List<JobPosition> list = jobPositionRepository.findAllWithDepartment();

        return list.stream()
                .filter(p -> {
                    if (Boolean.TRUE.equals(activeOnly) && !Boolean.TRUE.equals(p.getActive())) {
                        return false;
                    }
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(s);
                        boolean matchCode = p.getCode() != null && p.getCode().toLowerCase().contains(s);
                        boolean matchLevel = p.getLevel() != null && p.getLevel().toLowerCase().contains(s);
                        return matchName || matchCode || matchLevel;
                    }
                    return true;
                })
                .map(p -> toResponse(p, canViewSalary))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobPositionResponse getPositionById(Long id) {
        JobPosition position = jobPositionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + id));
        return toResponse(position, canCurrentUserViewSalary());
    }

    @Override
    @Transactional
    public JobPositionResponse createPosition(JobPositionRequest request) {
        validateRequest(request, null);

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với ID: " + request.getDepartmentId()));
        }

        CompetencyFramework framework = null;
        if (request.getCompetencyFrameworkId() != null) {
            framework = competencyFrameworkRepository.findById(request.getCompetencyFrameworkId())
                    .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + request.getCompetencyFrameworkId()));
        }

        JobPosition position = JobPosition.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .level(request.getLevel().trim())
                .department(dept)
                .competencyFramework(framework)
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        JobPosition saved = jobPositionRepository.save(position);
        log.info("Đã tạo mới chức danh: {} [{}]", saved.getName(), saved.getCode());
        return toResponse(saved, true);
    }

    @Override
    @Transactional
    public JobPositionResponse updatePosition(Long id, JobPositionRequest request) {
        JobPosition position = jobPositionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + id));

        validateRequest(request, id);

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với ID: " + request.getDepartmentId()));
        }

        CompetencyFramework framework = null;
        if (request.getCompetencyFrameworkId() != null) {
            framework = competencyFrameworkRepository.findById(request.getCompetencyFrameworkId())
                    .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + request.getCompetencyFrameworkId()));
        }

        position.setCode(request.getCode().trim().toUpperCase());
        position.setName(request.getName().trim());
        position.setLevel(request.getLevel().trim());
        position.setDepartment(dept);
        position.setCompetencyFramework(framework);
        position.setMinSalary(request.getMinSalary());
        position.setMaxSalary(request.getMaxSalary());
        position.setDescription(request.getDescription());
        if (request.getActive() != null) {
            position.setActive(request.getActive());
        }

        JobPosition updated = jobPositionRepository.save(position);
        log.info("Đã cập nhật chức danh: {} [{}]", updated.getName(), updated.getCode());
        return toResponse(updated, true);
    }

    @Override
    @Transactional
    public JobPositionResponse toggleStatus(Long id, Boolean active) {
        JobPosition position = jobPositionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + id));

        boolean newActive = active != null ? active : !Boolean.TRUE.equals(position.getActive());
        position.setActive(newActive);
        JobPosition saved = jobPositionRepository.save(position);
        log.info("Đã đổi trạng thái chức danh {} sang {}", saved.getCode(), newActive ? "Áp dụng" : "Ngừng áp dụng");
        return toResponse(saved, canCurrentUserViewSalary());
    }

    @Override
    @Transactional
    public void deletePosition(Long id) {
        JobPosition position = jobPositionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + id));
        jobPositionRepository.delete(position);
        log.info("Đã xóa chức danh: {} [{}]", position.getName(), position.getCode());
    }

    @Override
    public boolean canCurrentUserViewSalary() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_HR_MANAGER".equals(role) || "ROLE_ADMIN".equals(role)) {
                return true;
            }
        }
        return false;
    }

    private void validateRequest(JobPositionRequest request, Long currentId) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new AppException("Mã chức danh không được để trống.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException("Tên chức danh không được để trống.");
        }
        if (request.getLevel() == null || request.getLevel().trim().isEmpty()) {
            throw new AppException("Cấp bậc chức danh không được để trống.");
        }

        String code = request.getCode().trim().toUpperCase();
        boolean codeExists = currentId == null
                ? jobPositionRepository.existsByCodeIgnoreCase(code)
                : jobPositionRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (codeExists) {
            throw new AppException("Mã chức danh '" + code + "' đã tồn tại trên hệ thống.");
        }

        // KN-178: Kiểm tra tính hợp lệ của dải lương tối thiểu / tối đa
        if (request.getMinSalary() != null && request.getMinSalary() < 0) {
            throw new AppException("Mức lương tối thiểu không được âm.");
        }
        if (request.getMaxSalary() != null && request.getMaxSalary() < 0) {
            throw new AppException("Mức lương tối đa không được âm.");
        }
        if (request.getMinSalary() != null && request.getMaxSalary() != null) {
            if (request.getMinSalary() > request.getMaxSalary()) {
                throw new AppException(String.format(
                        "Dải lương không hợp lệ: Lương tối thiểu (%,.0f đ) không được lớn hơn lương tối đa (%,.0f đ).",
                        request.getMinSalary(), request.getMaxSalary()
                ));
            }
        }
    }

    private JobPositionResponse toResponse(JobPosition pos, boolean canViewSalary) {
        Department dept = pos.getDepartment();
        CompetencyFramework framework = pos.getCompetencyFramework();

        Double minSalary = canViewSalary ? pos.getMinSalary() : null;
        Double maxSalary = canViewSalary ? pos.getMaxSalary() : null;
        boolean isSalaryMasked = !canViewSalary && (pos.getMinSalary() != null || pos.getMaxSalary() != null);

        return JobPositionResponse.builder()
                .id(pos.getId())
                .code(pos.getCode())
                .name(pos.getName())
                .level(pos.getLevel())
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : null)
                .competencyFrameworkId(framework != null ? framework.getId() : null)
                .competencyFrameworkName(framework != null ? framework.getName() : null)
                .competencyFrameworkCode(framework != null ? framework.getCode() : null)
                .minSalary(minSalary)
                .maxSalary(maxSalary)
                .isSalaryMasked(isSalaryMasked)
                .description(pos.getDescription())
                .active(pos.getActive() != null ? pos.getActive() : true)
                .createdAt(pos.getCreatedAt())
                .updatedAt(pos.getUpdatedAt())
                .build();
    }
}

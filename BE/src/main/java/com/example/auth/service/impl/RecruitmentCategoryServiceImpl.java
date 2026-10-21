package com.example.auth.service.impl;

import com.example.auth.dto.RecruitmentCategoryRequest;
import com.example.auth.dto.RecruitmentCategoryResponse;
import com.example.auth.dto.ReorderCategoryItemDto;
import com.example.auth.entity.CategoryType;
import com.example.auth.entity.RecruitmentCategory;
import com.example.auth.exception.AppException;
import com.example.auth.repository.RecruitmentCategoryRepository;
import com.example.auth.service.RecruitmentCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitmentCategoryServiceImpl implements RecruitmentCategoryService {

    private final RecruitmentCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentCategoryResponse> getCategories(CategoryType type, Boolean activeOnly, String search) {
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Boolean activeFilter = Boolean.TRUE.equals(activeOnly) ? true : null;

        List<RecruitmentCategory> list = categoryRepository.searchCategories(type, activeFilter, normalizedSearch);

        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecruitmentCategoryResponse getCategoryById(Long id) {
        RecruitmentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy danh mục với ID: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional
    public RecruitmentCategoryResponse createCategory(RecruitmentCategoryRequest request) {
        String code = request.getCode().trim().toUpperCase();

        if (categoryRepository.existsByTypeAndCodeIgnoreCase(request.getType(), code)) {
            throw new AppException(String.format("Mã danh mục '%s' đã tồn tại trong nhóm '%s'", code, request.getType().getDisplayName()));
        }

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null || displayOrder < 1) {
            displayOrder = 1;
        }

        RecruitmentCategory category = RecruitmentCategory.builder()
                .type(request.getType())
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .displayOrder(displayOrder)
                .active(request.getActive() != null ? request.getActive() : true)
                .isSystem(false)
                .build();

        RecruitmentCategory saved = categoryRepository.save(category);
        log.info("Đã tạo mới danh mục tham chiếu: {} [{}] ({})", saved.getName(), saved.getCode(), saved.getType());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecruitmentCategoryResponse updateCategory(Long id, RecruitmentCategoryRequest request) {
        RecruitmentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy danh mục với ID: " + id));

        String code = request.getCode().trim().toUpperCase();
        if (categoryRepository.existsByTypeAndCodeIgnoreCaseAndIdNot(request.getType(), code, id)) {
            throw new AppException(String.format("Mã danh mục '%s' đã tồn tại trong nhóm '%s'", code, request.getType().getDisplayName()));
        }

        category.setType(request.getType());
        category.setCode(code);
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        if (request.getDisplayOrder() != null && request.getDisplayOrder() >= 1) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        RecruitmentCategory updated = categoryRepository.save(category);
        log.info("Đã cập nhật danh mục tham chiếu ID {}: {}", id, updated.getName());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void reorderCategories(List<ReorderCategoryItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (ReorderCategoryItemDto item : items) {
            categoryRepository.findById(item.getId()).ifPresent(cat -> {
                cat.setDisplayOrder(item.getDisplayOrder());
                categoryRepository.save(cat);
            });
        }
        log.info("Đã cập nhật thứ tự hiển thị cho {} danh mục tham chiếu", items.size());
    }

    @Override
    @Transactional
    public RecruitmentCategoryResponse toggleStatus(Long id, Boolean active) {
        RecruitmentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy danh mục với ID: " + id));

        boolean newActive = (active != null) ? active : !Boolean.TRUE.equals(category.getActive());
        category.setActive(newActive);
        RecruitmentCategory saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        RecruitmentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy danh mục với ID: " + id));

        // KN-192: Thiết lập cơ chế không cho xoá giá trị đã được tham chiếu
        long usageCount = getUsageCount(id);
        if (usageCount > 0) {
            throw new AppException(String.format(
                    "Không thể xóa giá trị danh mục '%s' vì đang được tham chiếu bởi %d hồ sơ/yêu cầu tuyển dụng. Vui lòng chuyển trạng thái sang 'Ngừng áp dụng' để bảo toàn dữ liệu lịch sử!",
                    category.getName(), usageCount
            ));
        }

        categoryRepository.delete(category);
        log.info("Đã xóa danh mục tham chiếu ID {}: {}", id, category.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUsageCount(Long id) {
        RecruitmentCategory category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return 0;
        }

        switch (category.getType()) {
            case CANDIDATE_SOURCE:
                return categoryRepository.countApplicationSourceReferences(id);
            case REJECTION_REASON:
                return categoryRepository.countApplicationRejectionReasonReferences(id);
            case WORK_LOCATION:
                return categoryRepository.countJobRequestLocationReferences(id);
            case EMPLOYMENT_TYPE:
                return categoryRepository.countJobRequestEmploymentTypeReferences(id);
            default:
                return 0;
        }
    }

    private RecruitmentCategoryResponse toResponse(RecruitmentCategory c) {
        long usage = getUsageCount(c.getId());

        return RecruitmentCategoryResponse.builder()
                .id(c.getId())
                .type(c.getType().name())
                .typeName(c.getType().getDisplayName())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .displayOrder(c.getDisplayOrder())
                .active(c.getActive())
                .isSystem(c.getIsSystem())
                .usageCount(usage)
                .canDelete(usage == 0)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

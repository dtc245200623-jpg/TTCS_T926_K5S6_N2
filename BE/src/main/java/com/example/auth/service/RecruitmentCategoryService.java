package com.example.auth.service;

import com.example.auth.dto.RecruitmentCategoryRequest;
import com.example.auth.dto.RecruitmentCategoryResponse;
import com.example.auth.dto.ReorderCategoryItemDto;
import com.example.auth.entity.CategoryType;

import java.util.List;

public interface RecruitmentCategoryService {

    // Lấy danh sách danh mục theo loại, trạng thái, từ khóa (KN-188, KN-189, KN-190, KN-191)
    List<RecruitmentCategoryResponse> getCategories(CategoryType type, Boolean activeOnly, String search);

    RecruitmentCategoryResponse getCategoryById(Long id);

    RecruitmentCategoryResponse createCategory(RecruitmentCategoryRequest request);

    RecruitmentCategoryResponse updateCategory(Long id, RecruitmentCategoryRequest request);

    // KN-193: Sắp xếp thứ tự hiển thị
    void reorderCategories(List<ReorderCategoryItemDto> items);

    RecruitmentCategoryResponse toggleStatus(Long id, Boolean active);

    // KN-192: Xóa danh mục - Chặn xóa nếu đang được tham chiếu
    void deleteCategory(Long id);

    // KN-192: Kiểm tra số lượng bản ghi đang tham chiếu
    long getUsageCount(Long id);
}

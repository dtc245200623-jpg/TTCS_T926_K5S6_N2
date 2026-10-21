package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.RecruitmentCategoryRequest;
import com.example.auth.dto.RecruitmentCategoryResponse;
import com.example.auth.dto.ReorderCategoryItemDto;
import com.example.auth.entity.CategoryType;
import com.example.auth.service.RecruitmentCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecruitmentCategoryController {

    private final RecruitmentCategoryService categoryService;

    /**
     * Lấy danh sách các loại danh mục hệ thống hỗ trợ
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getCategoryTypes() {
        List<Map<String, String>> types = Arrays.stream(CategoryType.values())
                .map(t -> Map.of("type", t.name(), "displayName", t.getDisplayName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách loại danh mục thành công", types));
    }

    /**
     * Lấy danh sách danh mục (hỗ trợ lọc theo loại, trạng thái, từ khóa)
     * Đã sắp xếp theo thứ tự hiển thị (KN-193)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentCategoryResponse>>> getCategories(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<RecruitmentCategoryResponse> list = categoryService.getCategories(type, activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", list));
    }

    /**
     * KN-188: Lấy danh mục nguồn ứng viên
     */
    @GetMapping("/sources")
    public ResponseEntity<ApiResponse<List<RecruitmentCategoryResponse>>> getCandidateSources(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<RecruitmentCategoryResponse> list = categoryService.getCategories(CategoryType.CANDIDATE_SOURCE, activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục nguồn ứng viên thành công", list));
    }

    /**
     * KN-189: Lấy danh mục lý do loại hồ sơ
     */
    @GetMapping("/rejection-reasons")
    public ResponseEntity<ApiResponse<List<RecruitmentCategoryResponse>>> getRejectionReasons(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<RecruitmentCategoryResponse> list = categoryService.getCategories(CategoryType.REJECTION_REASON, activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục lý do loại hồ sơ thành công", list));
    }

    /**
     * KN-190: Lấy danh mục địa điểm làm việc
     */
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<RecruitmentCategoryResponse>>> getWorkLocations(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<RecruitmentCategoryResponse> list = categoryService.getCategories(CategoryType.WORK_LOCATION, activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục địa điểm làm việc thành công", list));
    }

    /**
     * KN-191: Lấy danh mục hình thức làm việc
     */
    @GetMapping("/employment-types")
    public ResponseEntity<ApiResponse<List<RecruitmentCategoryResponse>>> getEmploymentTypes(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<RecruitmentCategoryResponse> list = categoryService.getCategories(CategoryType.EMPLOYMENT_TYPE, activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục hình thức làm việc thành công", list));
    }

    /**
     * Lấy chi tiết một giá trị danh mục
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecruitmentCategoryResponse>> getCategoryById(@PathVariable Long id) {
        RecruitmentCategoryResponse res = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết danh mục thành công", res));
    }

    /**
     * Thêm mới giá trị danh mục
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RecruitmentCategoryResponse>> createCategory(
            @Valid @RequestBody RecruitmentCategoryRequest request) {
        RecruitmentCategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo mới giá trị danh mục thành công", created));
    }

    /**
     * Cập nhật thông tin và thứ tự hiển thị của giá trị danh mục
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RecruitmentCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody RecruitmentCategoryRequest request) {
        RecruitmentCategoryResponse updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", updated));
    }

    /**
     * KN-193: Cập nhật sắp xếp thứ tự hiển thị hàng loạt
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderCategories(
            @Valid @RequestBody List<ReorderCategoryItemDto> items) {
        categoryService.reorderCategories(items);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thứ tự hiển thị thành công", null));
    }

    /**
     * Bật / Tắt trạng thái kích hoạt của giá trị danh mục
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RecruitmentCategoryResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active) {
        RecruitmentCategoryResponse res = categoryService.toggleStatus(id, active);
        String msg = Boolean.TRUE.equals(res.getActive()) ? "Kích hoạt áp dụng" : "Ngừng áp dụng";
        return ResponseEntity.ok(ApiResponse.success(msg + " thành công", res));
    }

    /**
     * KN-192: Xóa giá trị danh mục (Thiết lập cơ chế chặn xóa nếu đang được tham chiếu)
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa giá trị danh mục thành công", null));
    }
}

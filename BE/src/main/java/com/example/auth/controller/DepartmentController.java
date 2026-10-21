package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.DepartmentManagerDto;
import com.example.auth.dto.DepartmentRequest;
import com.example.auth.dto.DepartmentResponse;
import com.example.auth.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * KN-174: Lấy danh sách phòng ban theo cấu trúc cây nhiều cấp (Hierarchical Tree)
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'HIRING_MANAGER', 'RECRUITER', 'APPROVER', 'INTERVIEWER')")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartmentTree(
            @RequestParam(required = false) Boolean activeOnly) {
        List<DepartmentResponse> tree = departmentService.getDepartmentTree(activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu trúc cây phòng ban thành công", tree));
    }

    /**
     * Lấy danh sách phòng ban dạng phẳng (hỗ trợ tìm kiếm, lọc theo trạng thái)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'HIRING_MANAGER', 'RECRUITER', 'APPROVER', 'INTERVIEWER')")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<DepartmentResponse> list = departmentService.getAllDepartments(activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng ban thành công", list));
    }

    /**
     * Lấy thông tin chi tiết một phòng ban
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng ban thành công", response));
    }

    /**
     * Lấy danh sách nhân viên để chọn làm người phụ trách (KN-175)
     */
    @GetMapping("/managers")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentManagerDto>>> getEligibleManagers() {
        List<DepartmentManagerDto> managers = departmentService.getEligibleManagers();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhân viên phụ trách thành công", managers));
    }

    /**
     * Tạo mới phòng ban (hỗ trợ chọn phòng ban cha và người phụ trách)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse created = departmentService.createDepartment(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo mới phòng ban thành công", created));
    }

    /**
     * Cập nhật thông tin phòng ban
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse updated = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng ban thành công", updated));
    }

    /**
     * KN-175: Gán người phụ trách cho phòng ban
     */
    @PutMapping("/{id}/assign-manager")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> assignManager(
            @PathVariable Long id,
            @RequestParam(required = false) Long managerId) {
        DepartmentResponse updated = departmentService.assignManager(id, managerId);
        return ResponseEntity.ok(ApiResponse.success("Gán người phụ trách thành công", updated));
    }

    /**
     * KN-176: Chuyển đổi trạng thái áp dụng (Ngừng áp dụng / Kích hoạt lại)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active) {
        DepartmentResponse updated = departmentService.toggleStatus(id, active);
        String statusText = Boolean.TRUE.equals(updated.getActive()) ? "Kích hoạt áp dụng" : "Ngừng áp dụng";
        return ResponseEntity.ok(ApiResponse.success(statusText + " phòng ban thành công", updated));
    }

    /**
     * KN-176: Xóa phòng ban (Kiểm tra quy tắc không xóa nếu có yêu cầu tuyển dụng mở)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng ban thành công"));
    }
}

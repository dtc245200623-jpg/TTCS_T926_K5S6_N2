package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.DepartmentHeadcountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/headcount-plans")
@RequiredArgsConstructor
public class DepartmentHeadcountController {

    private final DepartmentHeadcountService headcountService;

    /**
     * KN-223: Khai báo / Cập nhật chỉ tiêu headcount và ngân sách lương theo phòng ban theo năm
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentHeadcountPlanDto>> createOrUpdatePlan(
            @RequestBody DepartmentHeadcountPlanDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "System";
        DepartmentHeadcountPlanDto result = headcountService.createOrUpdatePlan(dto, username);
        return ResponseEntity.ok(ApiResponse.success("Lưu kế hoạch chỉ tiêu headcount thành công!", result));
    }

    /**
     * Lấy danh sách chỉ tiêu theo năm hoặc toàn bộ
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentHeadcountPlanDto>>> getPlans(
            @RequestParam(required = false) Integer year) {
        List<DepartmentHeadcountPlanDto> list = year != null 
                ? headcountService.getPlansByYear(year)
                : headcountService.getAllPlans();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chỉ tiêu thành công!", list));
    }

    /**
     * Lấy chỉ tiêu của 1 phòng ban theo năm
     */
    @GetMapping("/by-dept")
    public ResponseEntity<ApiResponse<DepartmentHeadcountPlanDto>> getPlanByDepartment(
            @RequestParam Long departmentId,
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        DepartmentHeadcountPlanDto plan = headcountService.getPlanByDepartmentAndYear(departmentId, year);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chỉ tiêu thành công!", plan));
    }

    /**
     * Xóa chỉ tiêu
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        headcountService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chỉ tiêu thành công!", null));
    }

    /**
     * KN-224, KN-225: Kiểm tra số headcount còn lại của phòng ban và cảnh báo nếu vượt chỉ tiêu
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<HeadcountCheckResponseDto>> checkHeadcount(
            @RequestParam Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer targetYear,
            @RequestParam(required = false) Integer requestedHeadcount,
            @RequestParam(required = false) Integer requestedCount,
            @RequestParam(required = false) Long excludeJobRequestId) {
        Integer effectiveYear = targetYear != null ? targetYear : (year != null ? year : LocalDate.now().getYear());
        Integer effectiveReq = requestedHeadcount != null ? requestedHeadcount : (requestedCount != null ? requestedCount : 0);
        HeadcountCheckResponseDto check = headcountService.checkDepartmentHeadcount(
                departmentId, effectiveYear, effectiveReq, excludeJobRequestId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra chỉ tiêu thành công!", check));
    }

    /**
     * KN-226: Trưởng phòng Nhân sự xác nhận hoặc từ chối ghi đè chỉ tiêu headcount
     */
    @PostMapping("/override-action")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobRequestDto>> processOverrideAction(
            @RequestBody HeadcountOverrideActionRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "System";
        JobRequestDto result = headcountService.processHeadcountOverrideAction(request, username);
        return ResponseEntity.ok(ApiResponse.success("Xử lý xác nhận ghi đè chỉ tiêu thành công!", result));
    }

    /**
     * KN-227: Lấy lịch sử phê duyệt và ghi đè chỉ tiêu headcount phục vụ truy vết / kiểm toán
     */
    @GetMapping("/override-audits")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<HeadcountOverrideAuditDto>>> getOverrideAudits(
            @RequestParam(required = false) Long jobRequestId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year) {
        List<HeadcountOverrideAuditDto> list = headcountService.getOverrideAudits(jobRequestId, departmentId, year);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kiểm toán thành công!", list));
    }
}

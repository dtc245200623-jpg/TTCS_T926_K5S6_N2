package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.JobPositionRequest;
import com.example.auth.dto.JobPositionResponse;
import com.example.auth.service.JobPositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobPositionController {

    private final JobPositionService jobPositionService;

    /**
     * KN-177 & KN-180: Lấy danh sách chức danh.
     * Dải lương chỉ hiển thị cho Trưởng phòng Nhân sự và Admin (KN-180).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobPositionResponse>>> getAllPositions(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search) {
        List<JobPositionResponse> list = jobPositionService.getAllPositions(activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chức danh thành công", list));
    }

    /**
     * Lấy chi tiết chức danh theo ID (Dải lương bảo mật theo quyền KN-180)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPositionResponse>> getPositionById(@PathVariable Long id) {
        JobPositionResponse response = jobPositionService.getPositionById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chức danh thành công", response));
    }

    /**
     * KN-177 & KN-178: Tạo chức danh mới kèm dải lương tối thiểu / tối đa (vai trò Trưởng phòng Nhân sự)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobPositionResponse>> createPosition(
            @Valid @RequestBody JobPositionRequest request) {
        JobPositionResponse created = jobPositionService.createPosition(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo chức danh mới thành công", created));
    }

    /**
     * Cập nhật thông tin chức danh & dải lương
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobPositionResponse>> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody JobPositionRequest request) {
        JobPositionResponse updated = jobPositionService.updatePosition(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chức danh thành công", updated));
    }

    /**
     * Bật / Tắt trạng thái áp dụng chức danh
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobPositionResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active) {
        JobPositionResponse updated = jobPositionService.toggleStatus(id, active);
        String msg = Boolean.TRUE.equals(updated.getActive()) ? "Kích hoạt áp dụng chức danh" : "Ngừng áp dụng chức danh";
        return ResponseEntity.ok(ApiResponse.success(msg + " thành công", updated));
    }

    /**
     * Xóa chức danh
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePosition(@PathVariable Long id) {
        jobPositionService.deletePosition(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chức danh thành công"));
    }
}

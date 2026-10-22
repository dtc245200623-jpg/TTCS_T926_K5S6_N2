package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.entity.CategoryType;
import com.example.auth.repository.DepartmentRepository;
import com.example.auth.repository.RecruitmentCategoryRepository;
import com.example.auth.service.CandidateApplicationService;
import com.example.auth.service.CareerPortalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CareerPortalController {

    private final CareerPortalService careerPortalService;
    private final CandidateApplicationService candidateApplicationService;
    private final DepartmentRepository departmentRepository;
    private final RecruitmentCategoryRepository categoryRepository;

    /**
     * Lấy cấu hình trang việc làm công khai (Public API)
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<CareerPortalConfigResponse>> getPortalConfig() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin trang việc làm thành công",
                careerPortalService.getPortalConfig()
        ));
    }

    /**
     * Cập nhật thông tin cấu hình trang việc làm (HR_MANAGER hoặc ADMIN)
     */
    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CareerPortalConfigResponse>> updatePortalConfig(
            @Valid @RequestBody CareerPortalConfigRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "HR Manager";
        return ResponseEntity.ok(ApiResponse.success(
                "Lưu và xuất bản trang việc làm thành công",
                careerPortalService.updatePortalConfig(request, username)
        ));
    }

    /**
     * Tải lên logo công ty cho trang công khai (HR_MANAGER hoặc ADMIN)
     */
    @PostMapping(value = "/upload-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        String url = careerPortalService.uploadLogo(file);
        return ResponseEntity.ok(ApiResponse.success(
                "Tải lên logo thành công",
                Map.of("logoUrl", url)
        ));
    }

    /**
     * Tải lên ảnh bìa/banner cho trang công khai (HR_MANAGER hoặc ADMIN)
     */
    @PostMapping(value = "/upload-banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN') or hasAuthority('ROLE_HR_MANAGER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBanner(
            @RequestParam("file") MultipartFile file) {
        String url = careerPortalService.uploadBanner(file);
        return ResponseEntity.ok(ApiResponse.success(
                "Tải lên ảnh bìa thành công",
                Map.of("bannerUrl", url)
        ));
    }

    /**
     * Lấy danh mục phục vụ bộ lọc tìm kiếm công khai (Phòng ban, Địa điểm, Hình thức làm việc, Nguồn ứng viên)
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicFilterCategories() {
        var depts = departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "code", d.getCode()))
                .collect(Collectors.toList());

        var locations = categoryRepository.findByTypeAndActiveTrueOrderByDisplayOrderAsc(CategoryType.WORK_LOCATION).stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "code", c.getCode()))
                .collect(Collectors.toList());

        var employmentTypes = categoryRepository.findByTypeAndActiveTrueOrderByDisplayOrderAsc(CategoryType.EMPLOYMENT_TYPE).stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "code", c.getCode()))
                .collect(Collectors.toList());

        var sources = categoryRepository.findByTypeAndActiveTrueOrderByDisplayOrderAsc(CategoryType.CANDIDATE_SOURCE).stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "code", c.getCode()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh mục thành công",
                Map.of(
                        "departments", depts,
                        "locations", locations,
                        "employmentTypes", employmentTypes,
                        "sources", sources
                )
        ));
    }

    /**
     * Danh sách việc làm công khai với bộ lọc và tìm kiếm theo từ khoá (Image 1, KN-274)
     * Chỉ hiển thị tin đã xuất bản và còn hạn nhận hồ sơ
     */
    @GetMapping("/public-jobs")
    public ResponseEntity<ApiResponse<List<PublicJobDetailDto>>> getPublicJobs(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "locationId", required = false) Long locationId,
            @RequestParam(value = "employmentTypeId", required = false) Long employmentTypeId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<PublicJobDetailDto> jobs = candidateApplicationService.searchPublicJobs(departmentId, locationId, employmentTypeId, keyword);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách việc làm công khai thành công",
                jobs
        ));
    }

    /**
     * Xem chi tiết tin tuyển dụng cho ứng viên (KN-272, KN-273, KN-275, KN-277)
     * Tuyệt đối ẩn thông tin nội bộ
     */
    @GetMapping("/public-jobs/{id}")
    public ResponseEntity<ApiResponse<PublicJobDetailDto>> getPublicJobDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết tin tuyển dụng thành công",
                candidateApplicationService.getPublicJobDetail(id)
        ));
    }

    /**
     * Biểu mẫu nộp hồ sơ ứng tuyển trực tuyến không cần đăng nhập (KN-279 -> KN-285, KN-286 -> KN-290)
     */
    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateApplyResponseDto>> applyForJob(
            @Valid @ModelAttribute CandidateApplyRequestDto request,
            @RequestParam(value = "cvFile", required = false) MultipartFile cvFile,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);
        CandidateApplyResponseDto res = candidateApplicationService.submitApplication(request, cvFile, clientIp);
        return ResponseEntity.ok(ApiResponse.success(res.getMessage(), res));
    }

    /**
     * Tra cứu trạng thái hồ sơ bằng mã tra cứu và email (KN-291, KN-292)
     */
    @PostMapping("/track-status")
    public ResponseEntity<ApiResponse<PublicApplicationStatusDto>> trackApplicationStatus(
            @Valid @RequestBody ApplicationTrackingRequestDto request) {
        PublicApplicationStatusDto status = candidateApplicationService.trackApplicationStatus(request);
        return ResponseEntity.ok(ApiResponse.success("Tra cứu trạng thái hồ sơ thành công", status));
    }

    /**
     * KN-293, KN-294: Lấy thông tin ứng tuyển trước khi ứng viên xác nhận rút hồ sơ
     */
    @GetMapping("/withdraw-info")
    public ResponseEntity<ApiResponse<CandidateWithdrawInfoDto>> getWithdrawalInfo(
            @RequestParam("code") String code,
            @RequestParam("email") String email) {
        CandidateWithdrawInfoDto info = candidateApplicationService.getWithdrawalInfo(code, email);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin rút hồ sơ thành công", info));
    }

    /**
     * KN-293, KN-294, KN-295, KN-296: Thực hiện rút hồ sơ ứng tuyển
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<PublicApplicationStatusDto>> withdrawApplication(
            @Valid @RequestBody CandidateWithdrawRequestDto request) {
        PublicApplicationStatusDto result = candidateApplicationService.withdrawApplication(request);
        return ResponseEntity.ok(ApiResponse.success("Rút hồ sơ ứng tuyển thành công", result));
    }

    /**
     * Recruiter xem dữ liệu bóc tách từ CV đối chiếu với thông tin ứng viên (KN-287)
     */
    @GetMapping("/applications/{id}/parsed-data")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN', 'HR_MANAGER') or hasAuthority('ROLE_RECRUITER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_HR_MANAGER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> getApplicationParsedData(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dữ liệu bóc tách hồ sơ thành công",
                candidateApplicationService.getApplicationParsedData(id)
        ));
    }

    /**
     * Recruiter xác nhận hoặc chỉnh sửa dữ liệu bóc tách từ CV (KN-287, KN-288)
     */
    @PutMapping("/applications/{id}/confirm-parsed-data")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN', 'HR_MANAGER') or hasAuthority('ROLE_RECRUITER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_HR_MANAGER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> confirmParsedData(
            @PathVariable Long id,
            @RequestBody ConfirmParsedDataDto dto,
            Authentication authentication) {
        String recruiter = authentication != null ? authentication.getName() : "Recruiter";
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật dữ liệu bóc tách thành công",
                candidateApplicationService.confirmOrUpdateParsedData(id, dto, recruiter)
        ));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN_IP";
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.trim().isEmpty()) {
            return xf.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}

package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.UserRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // Yêu cầu quyền ROLE_ADMIN cho tất cả các API quản lý tài khoản
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", 
                userService.getAllUsers(search, role, status, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản thành công", userService.createUser(request)));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id, 
            @RequestBody Set<String> roles) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phân quyền thành công", userService.updateUserRoles(id, roles)));
    }

    @PutMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleLockUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Thay đổi trạng thái khóa tài khoản thành công", userService.toggleLockUser(id)));
    }
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> adminChangePassword(
            @PathVariable Long id, 
            @RequestBody java.util.Map<String, String> request) {
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới không được để trống"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", userService.adminChangePassword(id, newPassword)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài khoản thành công", null));
    }

    /**
     * Tải tệp mẫu Excel hoặc CSV để nhập tài khoản hàng loạt
     */
    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        byte[] data = userService.generateImportTemplate(format);
        String filename = "xlsx".equalsIgnoreCase(format) ? "mau_nhap_tai_khoan.xlsx" : "mau_nhap_tai_khoan.csv";
        String contentType = "xlsx".equalsIgnoreCase(format) 
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" 
                : "text/csv; charset=UTF-8";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }

    /**
     * Xem trước và kiểm tra lỗi từng dòng từ tệp tải lên trước khi nhập
     */
    @PostMapping("/import/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<com.example.auth.dto.UserImportRowDto>>> previewImport(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Xem trước dữ liệu tệp thành công", userService.previewImportUsers(file)));
    }

    /**
     * Nhập danh sách tài khoản: bỏ qua các dòng lỗi, nhập các dòng hợp lệ và trả về báo cáo tổng kết
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.example.auth.dto.ImportSummaryResponse>> importUsers(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Nhập danh sách tài khoản thành công", userService.importUsers(file)));
    }

    /**
     * Lấy thông tin hồ sơ của tài khoản đang đăng nhập (nhân sự, nhân viên, quản trị...)
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa xác thực người dùng"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công", userService.getProfile(authentication.getName())));
    }

    /**
     * Cập nhật hồ sơ cá nhân:
     * - Sửa họ tên, số điện thoại, chức danh hiển thị (KN-166)
     * - Không tự đổi được email, phòng ban và vai trò (KN-167)
     * - Kiểm tra định dạng số điện thoại Việt Nam (KN-209)
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            org.springframework.security.core.Authentication authentication,
            @jakarta.validation.Valid @RequestBody com.example.auth.dto.UserProfileRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa xác thực người dùng"));
        }
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin hồ sơ thành công", userService.updateProfile(authentication.getName(), request)));
    }

    /**
     * Tải lên và cập nhật ảnh đại diện:
     * - Chấp nhận JPG/PNG tối đa 2MB (KN-200)
     * - Cắt ảnh vuông và tạo bản thu nhỏ (KN-201)
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            org.springframework.security.core.Authentication authentication,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa xác thực người dùng"));
        }
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", userService.updateAvatar(authentication.getName(), file)));
    }
}


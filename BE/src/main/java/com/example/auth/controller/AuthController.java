package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.dto.ChangePasswordResponse;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.auth.repository.SystemLogRepository;
import com.example.auth.entity.SystemLog;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final SystemLogRepository systemLogRepository;

    /**
     * Endpoint đăng nhập để lấy JWT token thử nghiệm
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<com.example.auth.dto.LoginResponse>> login(
            @Valid @RequestBody com.example.auth.dto.LoginRequest request,
            HttpServletRequest httpRequest) {
        com.example.auth.dto.LoginResponse response = authService.login(request);
        
        // Ghi log hệ thống
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = httpRequest.getRemoteAddr();
        }
        SystemLog log = SystemLog.builder()
                .username(request.getUsername())
                .action("Đăng nhập thành công")
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();
        systemLogRepository.save(log);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    /**
     * Endpoint đổi mật khẩu và thu hồi các phiên đăng nhập khác.
     * Yêu cầu người dùng phải gửi kèm JWT token hợp lệ trong header Authorization: Bearer <token>.
     *
     * @param authentication Đối tượng chứa thông tin người dùng đã xác thực từ Security Context
     * @param request        DTO chứa mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu
     * @return ApiResponse chứa ChangePasswordResponse kèm token mới cho phiên hiện tại
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        String currentUsername = authentication.getName();
        ChangePasswordResponse response = authService.changePassword(currentUsername, request);

        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", response));
    }

    /**
     * Endpoint chủ động thu hồi tất cả các phiên đăng nhập (Đăng xuất khỏi tất cả thiết bị).
     *
     * @param authentication Thông tin người dùng hiện tại
     * @return ApiResponse thông báo thành công
     */
    @PostMapping("/revoke-sessions")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(Authentication authentication) {
        String currentUsername = authentication.getName();
        authService.revokeAllSessions(currentUsername);

        return ResponseEntity.ok(ApiResponse.success("Đã thu hồi tất cả các phiên đăng nhập thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            Authentication authentication) {
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            String username = authentication != null ? authentication.getName() : null;
            authService.logout(token, username);
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<com.example.auth.dto.LoginResponse>> refreshToken(
            @RequestParam("refreshToken") String refreshToken) {
        com.example.auth.dto.LoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Gia hạn phiên thành công", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<com.example.auth.dto.UserResponse>> getCurrentUser(Authentication authentication) {
        com.example.auth.entity.User user = (com.example.auth.entity.User) authentication.getPrincipal();
        com.example.auth.dto.UserResponse response = com.example.auth.dto.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .title(user.getTitle())
                .avatarUrl(user.getAvatarUrl())
                .roles(user.getRoles())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody com.example.auth.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi mã OTP về email của bạn", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody com.example.auth.dto.ResetPasswordWithOtpRequest request) {
        authService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}

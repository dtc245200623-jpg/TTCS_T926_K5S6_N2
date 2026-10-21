package com.example.auth.service;

import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.dto.ChangePasswordResponse;

public interface AuthService {

    /**
     * Xác thực thông tin đăng nhập và sinh JWT chứa token_version hiện tại
     */
    com.example.auth.dto.LoginResponse login(com.example.auth.dto.LoginRequest request);

    /**
     * Thực hiện đổi mật khẩu cho người dùng hiện tại và thu hồi tất cả phiên làm việc khác
     *
     * @param username Tên đăng nhập của người dùng đang thực hiện yêu cầu
     * @param request  Dữ liệu gồm mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu
     * @return ChangePasswordResponse chứa thông báo và token mới cho phiên hiện tại
     */
    ChangePasswordResponse changePassword(String username, ChangePasswordRequest request);

    /**
     * Thu hồi tất cả các phiên đăng nhập đang hoạt động bằng cách tăng token_version
     *
     * @param username Tên đăng nhập của người dùng
     */
    void revokeAllSessions(String username);

    /**
     * Đăng xuất: Đưa token hiện tại vào blacklist và xóa refresh token
     */
    void logout(String token, String username);

    /**
     * Gia hạn phiên bằng refresh token
     */
    com.example.auth.dto.LoginResponse refreshToken(String refreshToken);

    /**
     * Yêu cầu cấp mã OTP qua email
     */
    void forgotPassword(com.example.auth.dto.ForgotPasswordRequest request);

    /**
     * Đặt lại mật khẩu bằng mã OTP
     */
    void resetPasswordWithOtp(com.example.auth.dto.ResetPasswordWithOtpRequest request);
}

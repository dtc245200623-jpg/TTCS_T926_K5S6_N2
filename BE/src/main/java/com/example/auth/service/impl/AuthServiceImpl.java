package com.example.auth.service.impl;

import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.dto.ChangePasswordResponse;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.TokenBlacklistRepository;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.entity.TokenBlacklist;
import com.example.auth.entity.RefreshToken;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.service.AuthService;
import com.example.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;

    @Override
    public com.example.auth.dto.LoginResponse login(com.example.auth.dto.LoginRequest request) {
        String loginId = request.getUsername();
        User user = userRepository.findByUsername(loginId)
                .orElseGet(() -> userRepository.findByEmail(loginId)
                        .orElseThrow(() -> new AppException("Tên đăng nhập hoặc mật khẩu không chính xác")));

        // Check lock status
        if (user.getLockTime() != null) {
            if (user.getLockTime().plusMinutes(15).isAfter(java.time.LocalDateTime.now())) {
                throw new AppException("Tài khoản đã bị khóa tạm thời. Vui lòng thử lại sau 15 phút.");
            } else {
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) {
                user.setLockTime(java.time.LocalDateTime.now());
                userRepository.save(user);
                throw new AppException("Tài khoản đã bị khóa tạm thời. Vui lòng thử lại sau 15 phút.");
            }
            int remainingAttempts = 5 - user.getFailedAttempts();
            userRepository.save(user);
            throw new AppException("Tên đăng nhập hoặc mật khẩu không chính xác. Bạn còn " + remainingAttempts + " lần đăng nhập sai trước khi bị khóa.");
        }

        if (user.getFailedAttempts() > 0 || user.getLockTime() != null) {
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user);
        
        // Sinh refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(7)) // 7 ngày
                .build();
        
        // Xoá refresh token cũ (nếu có) để chỉ duy trì 1 phiên hoặc cho phép nhiều phiên (ở đây xoá đi cho đơn giản 1 thiết bị, hoặc tuỳ nghiệp vụ. Ở đây cho phép nhiều phiên thì không xoá, nhưng trong bài này tạm thời lưu)
        // refreshTokenRepository.deleteByUser(user); // Nếu muốn 1 phiên duy nhất
        refreshTokenRepository.save(refreshToken);

        return com.example.auth.dto.LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .roles(user.getRoles())
                .tokenVersion(user.getTokenVersion())
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(String username, ChangePasswordRequest request) {
        // 1. Tìm thông tin người dùng
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        // 2. Xác thực mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Đổi mật khẩu thất bại: Mật khẩu hiện tại không đúng cho user [{}]", username);
            throw new AppException("Mật khẩu hiện tại không chính xác");
        }

        // 3. Kiểm tra xác nhận mật khẩu mới
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp với mật khẩu mới");
        }

        // 4. Kiểm tra mật khẩu mới không được trùng với mật khẩu hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        // 5. Cập nhật mật khẩu đã được mã hóa BCrypt
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // 6. TĂNG TOKEN_VERSION ĐỂ THU HỒI TẤT CẢ CÁC PHIÊN ĐĂNG NHẬP / TOKEN JWT CŨ
        long oldVersion = user.getTokenVersion();
        long newVersion = oldVersion + 1;
        user.setTokenVersion(newVersion);

        User savedUser = userRepository.save(user);

        log.info("Đổi mật khẩu thành công cho user [{}]. token_version được nâng từ {} lên {}",
                username, oldVersion, newVersion);

        // 7. Tạo JWT mới với token_version mới cho phiên hiện tại của người dùng
        String newAccessToken = jwtTokenProvider.generateToken(savedUser);

        return ChangePasswordResponse.builder()
                .message("Đổi mật khẩu thành công. Tất cả các phiên đăng nhập khác đã bị vô hiệu hóa.")
                .newTokenVersion(newVersion)
                .newAccessToken(newAccessToken)
                .build();
    }

    @Override
    @Transactional
    public void revokeAllSessions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("Đã thu hồi tất cả phiên đăng nhập của user [{}]. token_version mới: {}",
                username, user.getTokenVersion());
    }

    @Override
    @Transactional
    public void logout(String token, String username) {
        if (token != null) {
            // Lấy thời gian hết hạn của token
            java.util.Date expirationDate = jwtTokenProvider.extractClaims(token).getExpiration();
            LocalDateTime expiry = LocalDateTime.ofInstant(expirationDate.toInstant(), ZoneId.systemDefault());
            
            // Đưa token vào blacklist
            TokenBlacklist blacklist = TokenBlacklist.builder()
                    .token(token)
                    .expiryDate(expiry)
                    .build();
            tokenBlacklistRepository.save(blacklist);
            
            // Xóa refresh token của user
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                refreshTokenRepository.deleteByUser(user);
            }
            log.info("Đăng xuất thành công, token đưa vào blacklist cho user [{}]", username);
        }
    }

    @Override
    @Transactional
    public com.example.auth.dto.LoginResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new AppException("Refresh token không hợp lệ hoặc đã hết hạn"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new AppException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = refreshToken.getUser();
        String newToken = jwtTokenProvider.generateToken(user);

        // Gia hạn refresh token
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        return com.example.auth.dto.LoginResponse.builder()
                .token(newToken)
                .username(user.getUsername())
                .roles(user.getRoles())
                .tokenVersion(user.getTokenVersion())
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(com.example.auth.dto.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user != null) {
            // Generate 6-digit OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(999999));
            user.setResetOtp(otp);
            user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);

            // Send Email
            emailService.sendOtpEmail(user.getEmail(), otp);
        }
        // Nếu user không tồn tại, kết thúc im lặng để frontend vẫn hiện cùng một thông báo
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(com.example.auth.dto.ResetPasswordWithOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản với email này"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp())) {
            throw new AppException("Mã OTP không hợp lệ hoặc đã được sử dụng");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException("Mã OTP đã hết hạn");
        }

        // Change password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        
        // Revoke sessions
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }
}

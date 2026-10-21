package com.example.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã OTP Đặt Lại Mật Khẩu - Hệ thống Tuyển dụng");
        
        message.setText("Xin chào,\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản trên Hệ thống Tuyển dụng Nội bộ.\n\n"
                + "Mã OTP của bạn là: " + otpCode + "\n\n"
                + "Mã OTP này có hiệu lực trong 30 phút và chỉ được sử dụng một lần.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\n"
                + "Ban quản trị Hệ thống");

        mailSender.send(message);
    }

    public void sendActivationEmail(String toEmail, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Tài Khoản Mới - Hệ thống Tuyển dụng");
        
        message.setText("Xin chào,\n\n"
                + "Tài khoản của bạn trên Hệ thống Tuyển dụng Nội bộ đã được tạo.\n\n"
                + "Dưới đây là mật khẩu tạm thời để bạn đăng nhập lần đầu tiên:\n"
                + "Mật khẩu: " + tempPassword + "\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay để đảm bảo an toàn.\n\n"
                + "Trân trọng,\n"
                + "Ban quản trị Hệ thống");

        mailSender.send(message);
    }
}

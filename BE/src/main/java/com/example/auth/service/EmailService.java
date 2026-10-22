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

    /**
     * KN-293: Gửi email xác nhận nộp hồ sơ thành công kèm liên kết rút hồ sơ
     */
    public void sendApplicationConfirmationEmail(String toEmail, String candidateName, String jobTitle, String trackingCode, String withdrawLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Xác nhận tiếp nhận hồ sơ ứng tuyển - " + jobTitle);

            message.setText("Kính gửi " + candidateName + ",\n\n"
                    + "Cảm ơn bạn đã quan tâm và ứng tuyển vào vị trí: " + jobTitle + ".\n\n"
                    + "Thông tin hồ sơ của bạn đã được ghi nhận vào hệ thống Tuyển dụng:\n"
                    + "- Mã tra cứu hồ sơ: " + trackingCode + "\n"
                    + "- Email liên hệ: " + toEmail + "\n\n"
                    + "Bạn có thể tra cứu tiến độ hồ sơ bất kỳ lúc nào tại Cổng nghề nghiệp bằng mã tra cứu trên.\n\n"
                    + "--------------------------------------------------------\n"
                    + "NẾU BẠN MUỐN RÚT HỒ SƠ ỨNG TUYỂN:\n"
                    + "Trong trường hợp bạn đã có kế hoạch khác hoặc không còn nhu cầu tham gia tuyển dụng, bạn có thể rút hồ sơ trực tiếp qua liên kết bảo mật dưới đây:\n"
                    + withdrawLink + "\n"
                    + "(Hệ thống sẽ yêu cầu bạn xác nhận lại trước khi chính thức hoàn tất việc rút hồ sơ).\n"
                    + "--------------------------------------------------------\n\n"
                    + "Chúc bạn một ngày làm việc hiệu quả và nhiều niềm vui!\n\n"
                    + "Trân trọng,\n"
                    + "Bộ phận Tuyển dụng Nhân sự");

            mailSender.send(message);
        } catch (Exception e) {
            // Không làm gián đoạn luồng nộp hồ sơ nếu lỗi mail server cục bộ
            System.err.println(">>> [EmailService] Lỗi gửi email xác nhận: " + e.getMessage());
        }
    }

    /**
     * KN-296: Gửi email thông báo cho Recruiter phụ trách khi ứng viên rút hồ sơ
     */
    public void sendCandidateWithdrawalNotification(String recruiterEmail, String recruiterName, String candidateName, String candidateEmail, String jobTitle, String trackingCode, String reason) {
        if (recruiterEmail == null || recruiterEmail.trim().isEmpty()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recruiterEmail);
            message.setSubject("[Thông báo tuyển dụng] Ứng viên đã rút hồ sơ - " + jobTitle);

            String reasonText = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Không nêu lý do cụ thể";

            message.setText("Kính gửi " + (recruiterName != null ? recruiterName : "Chuyên viên Tuyển dụng") + ",\n\n"
                    + "Hệ thống thông báo ứng viên sau đây vừa thực hiện RÚT HỒ SƠ khỏi quy trình tuyển dụng:\n\n"
                    + "1. Thông tin ứng viên:\n"
                    + "   - Họ và tên: " + candidateName + "\n"
                    + "   - Email: " + candidateEmail + "\n"
                    + "   - Mã hồ sơ: " + trackingCode + "\n\n"
                    + "2. Thông tin vị trí tuyển dụng phụ trách:\n"
                    + "   - Vị trí: " + jobTitle + "\n"
                    + "   - Lý do rút hồ sơ: " + reasonText + "\n\n"
                    + "Hồ sơ của ứng viên đã được tự động chuyển sang trạng thái 'Ứng viên rút' và đã được ẩn khỏi pipeline đang hoạt động của bạn.\n\n"
                    + "Trân trọng,\n"
                    + "Hệ thống Tuyển dụng Tự động");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Lỗi gửi email thông báo cho recruiter: " + e.getMessage());
        }
    }
}

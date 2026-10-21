package com.example.auth;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    /**
     * Khởi tạo tài khoản mẫu để kiểm thử tính năng ngay khi khởi chạy
     */
    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String encodedPassword = passwordEncoder.encode("123456@");
            
            // Tạm thời: Reset toàn bộ mật khẩu của tất cả user về mặc định
            java.util.List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                u.setPassword(encodedPassword);
                u.setFailedAttempts(0);
                u.setLockTime(null);
            }
            userRepository.saveAll(allUsers);
            System.out.println(">>> Đã reset toàn bộ mật khẩu về 123456@");

            // Dữ liệu cho 7 vai trò hệ thống và các tài khoản yêu cầu
            String[][] userRoles = {
                {"quantrivien@ictu.edu.vn", "ROLE_ADMIN"}, // Sẽ được xử lý cấp full quyền ở dưới
                {"ungvien@ictu.edu.vn", "ROLE_CANDIDATE"},
                {"nhanvientuyendung@ictu.edu.vn", "ROLE_RECRUITER"},
                {"truongbophan@ictu.edu.vn", "ROLE_HIRING_MANAGER"},
                {"nguoiphongvan@ictu.edu.vn", "ROLE_INTERVIEWER"},
                {"truongphongnhansu@ictu.edu.vn", "ROLE_HR_MANAGER"},
                {"nguoiduyet@ictu.edu.vn", "ROLE_APPROVER"},
                {"quantrihethong@ictu.edu.vn", "ROLE_ADMIN"},
                {"dtc245200623@ictu.edu.vn", "ROLE_USER"},
                {"dtc245200624@ictu.edu.vn", "ROLE_USER"},
                {"dtc245200209@ictu.edu.vn", "ROLE_USER"},
                {"dtc245200288@ictu.edu.vn", "ROLE_USER"}
            };

            for (String[] data : userRoles) {
                String email = data[0];
                String role = data[1];
                
                String username = email.substring(0, email.indexOf("@"));
                
                java.util.Set<String> roles = new java.util.HashSet<>(java.util.List.of(role));
                // Cấp TẤT CẢ quyền cho quantrivien
                if ("quantrivien@ictu.edu.vn".equals(email)) {
                    roles = new java.util.HashSet<>(java.util.List.of(
                        "ROLE_CANDIDATE", "ROLE_RECRUITER", "ROLE_HIRING_MANAGER", 
                        "ROLE_INTERVIEWER", "ROLE_HR_MANAGER", "ROLE_APPROVER", "ROLE_ADMIN"
                    ));
                }

                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                    user = User.builder()
                            .username(username)
                            .email(email)
                            .password(encodedPassword)
                            .tokenVersion(1L)
                            .roles(roles)
                            .build();
                    userRepository.save(user);
                    System.out.println(">>> Đã khởi tạo người dùng: " + email + " với vai trò: " + roles.toString());
                } else if ("quantrivien@ictu.edu.vn".equals(email)) {
                    // Cập nhật đè full quyền cho quantrivien nếu đã tồn tại
                    user.setRoles(roles);
                    userRepository.save(user);
                }
            }
        };
        }

        @Bean
        public CommandLineRunner initJobRequests(com.example.auth.repository.JobRequestRepository jobRepo, UserRepository userRepo) {
            return args -> {
                if (jobRepo.count() == 0) {
                    User hm = userRepo.findByUsername("truongbophan").orElse(null);
                    if (hm != null) {
                        com.example.auth.entity.JobRequest job = new com.example.auth.entity.JobRequest();
                        job.setTitle("Lập trình viên Backend (Java)");
                        job.setHeadcount(2);
                        job.setMinSalary(15000000D);
                        job.setMaxSalary(30000000D);
                        job.setDescription("Phát triển backend với Java Spring Boot");
                        job.setRequirements("2 năm kinh nghiệm Java");
                        job.setHiringManager(hm);
                        job.setStatus("OPEN");
                        jobRepo.save(job);
                        System.out.println(">>> Đã tạo Job Request mẫu ID 1");
                    }
                }
            };
        }
}

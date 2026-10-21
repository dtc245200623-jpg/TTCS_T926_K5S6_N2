package com.example.auth;

import com.example.auth.dto.JobPositionRequest;
import com.example.auth.dto.JobPositionResponse;
import com.example.auth.dto.OfferDto;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.JobPositionService;
import com.example.auth.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JobPositionServiceTest {

    @Autowired
    private JobPositionService jobPositionService;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private String randomCode(String prefix) {
        return (prefix + "_" + UUID.randomUUID().toString().substring(0, 6)).toUpperCase();
    }

    private void authenticateAs(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, "password", List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void setUp() {
        authenticateAs("truongphongnhansu", "ROLE_HR_MANAGER");
    }

    @Test
    @DisplayName("KN-177: Xây dựng danh mục chức danh với mã, tên và cấp bậc")
    void testCreateJobPositionWithCodeNameLevel() {
        String code = randomCode("POS_DEV");
        JobPositionRequest req = JobPositionRequest.builder()
                .code(code)
                .name("Kỹ sư Lập trình Backend Senior")
                .level("Senior")
                .description("Phát triển dịch vụ cốt lõi")
                .minSalary(20000000.0)
                .maxSalary(35000000.0)
                .active(true)
                .build();

        JobPositionResponse resp = jobPositionService.createPosition(req);
        assertNotNull(resp.getId());
        assertEquals(code, resp.getCode());
        assertEquals("Kỹ sư Lập trình Backend Senior", resp.getName());
        assertEquals("Senior", resp.getLevel());
        assertTrue(resp.getActive());

        // Kiểm tra chống trùng mã chức danh
        JobPositionRequest duplicateCodeReq = JobPositionRequest.builder()
                .code(code)
                .name("Tên khác")
                .level("Junior")
                .build();
        assertThrows(AppException.class, () -> jobPositionService.createPosition(duplicateCodeReq));
    }

    @Test
    @DisplayName("KN-178: Triển khai quản lý dải lương tối thiểu/tối đa cho từng chức danh")
    void testSalaryBandValidation() {
        String code = randomCode("SALARY_TEST");

        // 1. minSalary > maxSalary -> Phải ném ngoại lệ
        JobPositionRequest invalidRangeReq = JobPositionRequest.builder()
                .code(code)
                .name("Chuyên viên Tuyển dụng")
                .level("Middle")
                .minSalary(30000000.0)
                .maxSalary(20000000.0)
                .build();
        AppException ex = assertThrows(AppException.class, () -> jobPositionService.createPosition(invalidRangeReq));
        assertTrue(ex.getMessage().contains("Dải lương không hợp lệ"));

        // 2. Lương âm -> Phải ném ngoại lệ
        JobPositionRequest negativeSalaryReq = JobPositionRequest.builder()
                .code(code)
                .name("Chuyên viên Tuyển dụng")
                .level("Middle")
                .minSalary(-5000000.0)
                .maxSalary(20000000.0)
                .build();
        assertThrows(AppException.class, () -> jobPositionService.createPosition(negativeSalaryReq));

        // 3. Dải lương hợp lệ -> Thành công
        JobPositionRequest validReq = JobPositionRequest.builder()
                .code(code)
                .name("Chuyên viên Tuyển dụng")
                .level("Middle")
                .minSalary(12000000.0)
                .maxSalary(18000000.0)
                .build();
        JobPositionResponse validResp = jobPositionService.createPosition(validReq);
        assertEquals(12000000.0, validResp.getMinSalary());
        assertEquals(18000000.0, validResp.getMaxSalary());
    }

    @Test
    @DisplayName("KN-179: Áp dụng giới hạn duyệt offer theo dải lương của chức danh")
    void testOfferApprovalSalaryBandLimit() {
        // 1. Tạo Chức danh có dải lương tối đa là 25,000,000 VND
        String code = randomCode("POS_LIMIT");
        JobPosition pos = JobPosition.builder()
                .code(code)
                .name("Lập trình viên ReactJS " + code)
                .level("Middle")
                .minSalary(15000000.0)
                .maxSalary(25000000.0)
                .active(true)
                .build();
        pos = jobPositionRepository.save(pos);

        // 2. Tạo JobRequest liên kết tới Chức danh này
        JobRequest jr = JobRequest.builder()
                .title("Tuyển ReactJS " + code)
                .position(pos)
                .minSalary(15000000.0)
                .maxSalary(25000000.0)
                .status("OPEN")
                .build();
        jr = jobRequestRepository.save(jr);

        // 3. Tạo Application
        Application app = Application.builder()
                .jobRequest(jr)
                .status("INTERVIEWING")
                .build();
        app = applicationRepository.save(app);

        // 4. Test Case A: Offer trong hạn mức (22,000,000 <= 25,000,000)
        OfferDto normalOffer = offerService.createOrSubmitOffer(app.getId(), 22000000.0, "Offer đạt chuẩn");
        assertFalse(normalOffer.getExceedsLimit());
        assertEquals(25000000.0, normalOffer.getMaxSalaryAllowed());

        // 5. Test Case B: Offer VƯỢT hạn mức (28,000,000 > 25,000,000)
        OfferDto exceedingOffer = offerService.createOrSubmitOffer(app.getId(), 28000000.0, "Ứng viên deal lương cao");
        assertTrue(exceedingOffer.getExceedsLimit());
        assertEquals(25000000.0, exceedingOffer.getMaxSalaryAllowed());

        // 6. Thử duyệt Offer vượt hạn mức khi chỉ là ROLE_HR_MANAGER (chưa phải Ban Giám Đốc APPROVER)
        authenticateAs("hr_user", "ROLE_HR_MANAGER");
        AppException ex = assertThrows(AppException.class, () -> offerService.approveOffer(exceedingOffer.getId()));
        assertTrue(ex.getMessage().contains("ROLE_APPROVER") || ex.getMessage().contains("vượt quá dải lương tối đa"));

        // 7. Duyệt Offer vượt hạn mức khi đăng nhập với quyền Ban Giám Đốc (ROLE_APPROVER)
        authenticateAs("giamdoc", "ROLE_APPROVER");
        assertDoesNotThrow(() -> offerService.approveOffer(exceedingOffer.getId(), "giamdoc", "Ban Giám Đốc đặc cách duyệt offer vượt hạn mức"));

        Offer approvedOffer = offerRepository.findById(exceedingOffer.getId()).orElseThrow();
        assertEquals("APPROVED", approvedOffer.getStatus());
        assertTrue(approvedOffer.getApprovalNote().contains("đặc cách"));
    }

    @Test
    @DisplayName("KN-180: Giới hạn hiển thị dải lương chỉ cho Trưởng phòng Nhân sự và Admin")
    void testSalaryVisibilityRestrictedToHrManager() {
        String code = randomCode("SEC_POS");
        JobPosition pos = JobPosition.builder()
                .code(code)
                .name("Giám đốc Tài chính " + code)
                .level("Director")
                .minSalary(50000000.0)
                .maxSalary(80000000.0)
                .active(true)
                .build();
        pos = jobPositionRepository.save(pos);

        // 1. Khi đăng nhập là ROLE_HR_MANAGER: Xem được dải lương
        authenticateAs("truongphong", "ROLE_HR_MANAGER");
        JobPositionResponse hrView = jobPositionService.getPositionById(pos.getId());
        assertEquals(50000000.0, hrView.getMinSalary());
        assertEquals(80000000.0, hrView.getMaxSalary());
        assertFalse(hrView.getIsSalaryMasked());

        // 2. Khi đăng nhập là ROLE_ADMIN: Xem được dải lương
        authenticateAs("admin_user", "ROLE_ADMIN");
        JobPositionResponse adminView = jobPositionService.getPositionById(pos.getId());
        assertEquals(50000000.0, adminView.getMinSalary());
        assertEquals(80000000.0, adminView.getMaxSalary());
        assertFalse(adminView.getIsSalaryMasked());

        // 3. Khi đăng nhập là vai trò khác (VD: ROLE_RECRUITER, ROLE_INTERVIEWER, ROLE_USER): Dải lương PHẢI BỊ ẨN
        authenticateAs("recruiter_user", "ROLE_RECRUITER");
        JobPositionResponse recruiterView = jobPositionService.getPositionById(pos.getId());
        assertNull(recruiterView.getMinSalary(), "Recruiter không được xem minSalary");
        assertNull(recruiterView.getMaxSalary(), "Recruiter không được xem maxSalary");
        assertTrue(recruiterView.getIsSalaryMasked(), "Phải đánh dấu isSalaryMasked = true");

        authenticateAs("employee_user", "ROLE_USER");
        JobPositionResponse userView = jobPositionService.getPositionById(pos.getId());
        assertNull(userView.getMinSalary(), "User thường không được xem minSalary");
        assertNull(userView.getMaxSalary(), "User thường không được xem maxSalary");
        assertTrue(userView.getIsSalaryMasked());
    }
}

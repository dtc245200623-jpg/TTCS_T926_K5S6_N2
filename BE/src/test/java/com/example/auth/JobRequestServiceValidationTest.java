package com.example.auth;

import com.example.auth.dto.JobRequestDto;
import com.example.auth.entity.Department;
import com.example.auth.entity.JobPosition;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.repository.DepartmentRepository;
import com.example.auth.repository.JobPositionRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.JobRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JobRequestServiceValidationTest {

    @Autowired
    private JobRequestService jobRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    private User testHiringManager;
    private Department testDept;
    private JobPosition testPosition;

    @BeforeEach
    void setUp() {
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_HIRING_MANAGER");

        testHiringManager = userRepository.findByUsername("test_hiring_manager").orElseGet(() -> {
            User u = new User();
            u.setUsername("test_hiring_manager");
            u.setPassword("encoded_pass");
            u.setFullName("Trưởng Bộ Phận Test");
            u.setEmail("hiring_manager_test@example.com");
            u.setRoles(roles);
            return userRepository.save(u);
        });

        testDept = departmentRepository.save(Department.builder()
                .code("DEPT_TEST_" + System.currentTimeMillis())
                .name("Phòng Phát Triển Phần Mềm")
                .active(true)
                .build());

        testPosition = jobPositionRepository.save(JobPosition.builder()
                .code("POS_DEV_" + System.currentTimeMillis())
                .name("Kỹ Sư Phần Mềm Cao Cấp")
                .level("Senior")
                .department(testDept)
                .minSalary(20_000_000.0)
                .maxSalary(30_000_000.0)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("KN-207: Soạn thảo và lưu nháp mô tả công việc, yêu cầu ứng viên thành công")
    void testCreateDraft_KN207_Success() {
        JobRequestDto draftDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .title("Tuyển dụng Senior Dev (Bản nháp)")
                .description("Mô tả đang soạn dở...")
                .build();

        JobRequestDto created = jobRequestService.createOrDraftJobRequest(draftDto, testHiringManager.getUsername(), true);

        assertNotNull(created.getId());
        assertEquals("DRAFT", created.getStatus());
        assertTrue(created.getIsDraft());
        assertEquals("Tuyển dụng Senior Dev (Bản nháp)", created.getTitle());
    }

    @Test
    @DisplayName("KN-208: Kiểm tra hợp lệ ngày cần người không được ở quá khứ (ném ngoại lệ)")
    void testCreateOfficial_KN208_PastNeededDate_ThrowsException() {
        JobRequestDto dto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(2)
                .recruitmentReason("REPLACEMENT")
                .neededDate(LocalDate.now().minusDays(1)) // Ngày ở quá khứ!
                .minSalary(22_000_000.0)
                .maxSalary(28_000_000.0)
                .description("Phát triển các microservice lõi.")
                .requirements("Java 17+, Spring Boot, Docker.")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                jobRequestService.createOrDraftJobRequest(dto, testHiringManager.getUsername(), false)
        );

        assertTrue(ex.getMessage().contains("Ngày cần người không được ở quá khứ"),
                "Thông báo lỗi phải cảnh báo ngày không được ở quá khứ. Thực tế: " + ex.getMessage());
    }

    @Test
    @DisplayName("KN-208: Ngày cần người không được để trống khi gửi phê duyệt")
    void testCreateOfficial_KN208_MissingNeededDate_ThrowsException() {
        JobRequestDto dto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(1)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(null) // Trống ngày cần người
                .minSalary(25_000_000.0)
                .maxSalary(28_000_000.0)
                .description("Mô tả công việc chi tiết")
                .requirements("Yêu cầu ứng viên chi tiết")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                jobRequestService.createOrDraftJobRequest(dto, testHiringManager.getUsername(), false)
        );

        assertTrue(ex.getMessage().contains("Vui lòng nhập ngày cần người"));
    }

    @Test
    @DisplayName("KN-208: Dải lương nằm ngoài dải chuẩn mà không có giải trình thì bắt buộc nhập giải trình")
    void testCreateOfficial_KN208_OutOfRangeSalary_NoExplanation_ThrowsException() {
        // Dải chuẩn chức danh là 20.000.000 - 30.000.000
        // Đề xuất mức tối đa 35.000.000 (vượt trần chuẩn) nhưng không có salaryExplanation
        JobRequestDto dto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(1)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(LocalDate.now().plusMonths(1))
                .minSalary(20_000_000.0)
                .maxSalary(35_000_000.0) // Vượt dải chuẩn
                .salaryExplanation(null)  // Không nhập giải trình
                .description("Phụ trách kiến trúc hệ thống phân tán")
                .requirements("Kinh nghiệm System Architect > 5 năm")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                jobRequestService.createOrDraftJobRequest(dto, testHiringManager.getUsername(), false)
        );

        assertTrue(ex.getMessage().contains("nằm ngoài dải chuẩn của chức danh") &&
                        ex.getMessage().contains("Bắt buộc phải nhập giải trình lý do"),
                "Phải yêu cầu bắt buộc nhập giải trình khi lương ngoài dải chuẩn. Thực tế: " + ex.getMessage());
    }

    @Test
    @DisplayName("KN-208: Dải lương nằm ngoài dải chuẩn nhưng CÓ giải trình hợp lệ thì thành công")
    void testCreateOfficial_KN208_OutOfRangeSalary_WithExplanation_Success() {
        JobRequestDto dto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(1)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(LocalDate.now().plusDays(15))
                .minSalary(18_000_000.0) // Dưới chuẩn
                .maxSalary(36_000_000.0) // Vượt chuẩn
                .salaryExplanation("Vị trí chuyên gia kiến trúc đặc biệt cần deal dải lương mở rộng theo năng lực")
                .description("Chủ trì thiết kế kiến trúc vi dịch vụ quy mô lớn")
                .requirements("Có chứng chỉ AWS Solution Architect & kinh nghiệm Kubernetes")
                .build();

        JobRequestDto created = jobRequestService.createOrDraftJobRequest(dto, testHiringManager.getUsername(), false);

        assertNotNull(created.getId());
        assertEquals("PENDING_APPROVAL", created.getStatus());
        assertEquals(36_000_000.0, created.getMaxSalary());
        assertNotNull(created.getSalaryExplanation());
        assertTrue(created.getSalaryExplanation().contains("Vị trí chuyên gia kiến trúc"));
    }

    @Test
    @DisplayName("KN-206: Khai báo lý do tuyển dụng (Thay thế hoặc Tăng mới) là bắt buộc khi gửi duyệt")
    void testCreateOfficial_KN206_MissingRecruitmentReason_ThrowsException() {
        JobRequestDto dto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(2)
                .recruitmentReason("") // Để trống lý do tuyển
                .neededDate(LocalDate.now().plusWeeks(2))
                .minSalary(22_000_000.0)
                .maxSalary(28_000_000.0)
                .description("Lập trình viên backend")
                .requirements("Java 17, Spring")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                jobRequestService.createOrDraftJobRequest(dto, testHiringManager.getUsername(), false)
        );

        assertTrue(ex.getMessage().contains("lý do tuyển dụng"));
    }

    @Test
    @DisplayName("KN-207: Soạn thảo lưu nháp sau đó gửi duyệt thành công")
    void testSubmitDraft_KN207_Success() {
        // 1. Tạo bản nháp
        JobRequestDto draftDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .title("Nháp tuyển Senior Java")
                .headcount(1)
                .recruitmentReason("REPLACEMENT")
                .neededDate(LocalDate.now().plusDays(30))
                .minSalary(22_000_000.0)
                .maxSalary(29_000_000.0)
                .description("Soạn thảo mô tả công việc đầy đủ")
                .requirements("Soạn thảo yêu cầu ứng viên đầy đủ")
                .build();

        JobRequestDto draft = jobRequestService.createOrDraftJobRequest(draftDto, testHiringManager.getUsername(), true);
        assertEquals("DRAFT", draft.getStatus());

        // 2. Gửi phê duyệt bản nháp
        JobRequestDto submitted = jobRequestService.submitJobRequest(draft.getId(), testHiringManager.getUsername());
        assertEquals("PENDING_APPROVAL", submitted.getStatus());
        assertFalse(submitted.getIsDraft());
    }

    @Test
    @DisplayName("KN-207: Xóa bản nháp yêu cầu tuyển dụng thành công")
    void testDeleteDraft_KN207_Success() {
        JobRequestDto draftDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .title("Bản nháp cần xóa")
                .build();

        JobRequestDto draft = jobRequestService.createOrDraftJobRequest(draftDto, testHiringManager.getUsername(), true);
        Long id = draft.getId();

        jobRequestService.deleteJobRequest(id, testHiringManager.getUsername());

        assertThrows(AppException.class, () -> jobRequestService.getJobRequestById(id));
    }

    @Test
    @DisplayName("KN-228, KN-229, KN-230: Sao chép yêu cầu tuyển dụng sang bản sao đầy đủ nội dung, không kế thừa lịch sử duyệt và mặc định là Nháp")
    void testCloneJobRequest_KN228_KN229_KN230_Success() {
        // 1. Tạo một yêu cầu tuyển dụng gốc có đầy đủ thông tin
        JobRequestDto originDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .title("Tuyển Kỹ sư Backend Lead")
                .headcount(2)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(LocalDate.now().plusDays(45))
                .minSalary(22_000_000.0)
                .maxSalary(28_000_000.0)
                .salaryExplanation("Lương chuẩn theo dải")
                .description("Phụ trách thiết kế kiến trúc hệ thống, lãnh đạo đội ngũ kỹ sư backend.")
                .requirements("Tối thiểu 5 năm kinh nghiệm Java, Spring Boot, Microservices.")
                .build();

        JobRequestDto submitted = jobRequestService.createOrDraftJobRequest(originDto, testHiringManager.getUsername(), false);
        assertNotNull(submitted.getId());
        assertEquals("PENDING_APPROVAL", submitted.getStatus());

        // 2. Thực hiện sao chép yêu cầu (Clone)
        JobRequestDto cloned = jobRequestService.cloneJobRequest(submitted.getId(), testHiringManager.getUsername());

        // KN-228: Sao chép đầy đủ nội dung yêu cầu tuyển dụng sang bản sao
        assertNotNull(cloned.getId());
        assertNotEquals(submitted.getId(), cloned.getId());
        assertTrue(cloned.getTitle().contains("Tuyển Kỹ sư Backend Lead"));
        assertEquals(submitted.getDepartmentId(), cloned.getDepartmentId());
        assertEquals(submitted.getPositionId(), cloned.getPositionId());
        assertEquals(submitted.getHeadcount(), cloned.getHeadcount());
        assertEquals(submitted.getRecruitmentReason(), cloned.getRecruitmentReason());
        assertEquals(submitted.getNeededDate(), cloned.getNeededDate());
        assertEquals(submitted.getMinSalary(), cloned.getMinSalary());
        assertEquals(submitted.getMaxSalary(), cloned.getMaxSalary());
        assertEquals(submitted.getSalaryExplanation(), cloned.getSalaryExplanation());
        assertEquals("Phụ trách thiết kế kiến trúc hệ thống, lãnh đạo đội ngũ kỹ sư backend.", cloned.getDescription());
        assertEquals("Tối thiểu 5 năm kinh nghiệm Java, Spring Boot, Microservices.", cloned.getRequirements());

        // KN-229: Không sao chép lịch sử phê duyệt khi tạo bản sao
        assertNull(cloned.getCurrentApprovalStep());
        assertNull(cloned.getTotalApprovalSteps());
        assertNull(cloned.getApprovalRuleId());
        assertTrue(cloned.getApprovalSteps() == null || cloned.getApprovalSteps().isEmpty());
        assertFalse(Boolean.TRUE.equals(cloned.getIsOverHeadcount()));
        assertFalse(Boolean.TRUE.equals(cloned.getOverHeadcountApproved()));
        assertNull(cloned.getOverHeadcountReason());
        assertNull(cloned.getOverHeadcountApprovedBy());

        // KN-230: Đặt trạng thái bản sao mặc định là Nháp
        assertEquals("DRAFT", cloned.getStatus());
        assertTrue(cloned.getIsDraft());
    }
}

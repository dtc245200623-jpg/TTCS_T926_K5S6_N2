package com.example.auth;

import com.example.auth.dto.CloseJobRequestDto;
import com.example.auth.dto.JobRequestDto;
import com.example.auth.dto.PublishJobPostingDto;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.CareerPortalService;
import com.example.auth.service.JobRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử toàn diện 6 Subtasks của User Story:
 * "Đăng tin & Cổng ứng tuyển - Nhân viên tuyển dụng"
 * Bao gồm: KN-253, KN-254, KN-255, KN-256, KN-257, KN-258
 */
@SpringBootTest
@Transactional
public class JobPublishingSubtasksTest {

    @Autowired
    private JobRequestService jobRequestService;

    @Autowired
    private CareerPortalService careerPortalService;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruitmentCategoryRepository categoryRepository;

    private User recruiter;
    private Department department;
    private JobPosition position;
    private RecruitmentCategory locationHanoi;
    private RecruitmentCategory employmentFulltime;
    private JobRequest approvedJobRequest;
    private JobRequest draftJobRequest;

    @BeforeEach
    void setUp() {
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_RECRUITER");

        recruiter = userRepository.findByUsername("recruiter_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("recruiter_test");
            u.setPassword("encoded_pass");
            u.setFullName("Lê Hoàng Long");
            u.setEmail("recruiter_test@example.com");
            u.setRoles(roles);
            return userRepository.save(u);
        });

        department = departmentRepository.save(Department.builder()
                .code("DEPT_KN253_" + System.currentTimeMillis())
                .name("Phòng Công Nghệ Lõi")
                .active(true)
                .build());

        position = jobPositionRepository.save(JobPosition.builder()
                .code("POS_KN253_" + System.currentTimeMillis())
                .name("Kỹ Sư Phần Mềm Cao Cấp")
                .level("Senior")
                .department(department)
                .minSalary(25_000_000.0)
                .maxSalary(35_000_000.0)
                .active(true)
                .build());

        locationHanoi = categoryRepository.save(RecruitmentCategory.builder()
                .code("LOC_HN_" + System.currentTimeMillis())
                .name("Trụ sở Hà Nội (Keangnam Landmark)")
                .type(CategoryType.WORK_LOCATION)
                .active(true)
                .displayOrder(1)
                .build());

        employmentFulltime = categoryRepository.save(RecruitmentCategory.builder()
                .code("EMP_FT_" + System.currentTimeMillis())
                .name("Toàn thời gian (Full-time)")
                .type(CategoryType.EMPLOYMENT_TYPE)
                .active(true)
                .displayOrder(1)
                .build());

        // Yêu cầu tuyển dụng đã duyệt (APPROVED)
        approvedJobRequest = jobRequestRepository.save(JobRequest.builder()
                .title("Yêu cầu tuyển dụng Senior Backend Dev")
                .department(department)
                .position(position)
                .headcount(2)
                .minSalary(28_000_000.0)
                .maxSalary(35_000_000.0)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(LocalDate.now().plusDays(45))
                .description("Mô tả nội bộ phòng ban về nhân sự Senior Java")
                .requirements("Yêu cầu nội bộ kỹ năng Spring Boot, SQL, 3 năm kinh nghiệm")
                .status("APPROVED")
                .hiringManager(recruiter)
                .createdAt(LocalDateTime.now())
                .build());

        // Yêu cầu tuyển dụng đang nháp (DRAFT)
        draftJobRequest = jobRequestRepository.save(JobRequest.builder()
                .title("Yêu cầu tuyển dụng nháp")
                .department(department)
                .position(position)
                .headcount(1)
                .status("DRAFT")
                .hiringManager(recruiter)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * KN-253: Tạo tin tuyển dụng từ yêu cầu tuyển dụng đã được phê duyệt
     */
    @Test
    @DisplayName("KN-253: Tạo tin thành công khi yêu cầu ở trạng thái APPROVED")
    void testKN253_PublishApprovedJob_Success() {
        PublishJobPostingDto dto = PublishJobPostingDto.builder()
                .title("[Hà Nội] Tuyển dụng Kỹ Sư Senior Java - Đãi ngộ lên đến 35M")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(30))
                .isSalaryVisible(true)
                .description("Mô tả công việc hấp dẫn...")
                .requirements("Yêu cầu kinh nghiệm 3+ năm...")
                .build();

        JobRequestDto result = jobRequestService.publishJobPosting(approvedJobRequest.getId(), dto, recruiter.getUsername());

        assertNotNull(result);
        assertEquals("OPEN", result.getStatus());
        assertEquals(locationHanoi.getName(), result.getLocationName());
        assertEquals(employmentFulltime.getName(), result.getEmploymentTypeName());
        assertTrue(result.getPublishedBy().equals(recruiter.getFullName()) || result.getPublishedBy().equals(recruiter.getUsername()));
        assertNotNull(result.getPublishedAt());
    }

    /**
     * KN-253 & Quy tắc 3: Chặn tạo tin tuyển dụng nếu yêu cầu chưa được phê duyệt (DRAFT / PENDING)
     */
    @Test
    @DisplayName("KN-253: Chặn tạo tin tuyển dụng nếu trạng thái không phải APPROVED (ném ngoại lệ)")
    void testKN253_PublishNonApprovedJob_ThrowsException() {
        PublishJobPostingDto dto = PublishJobPostingDto.builder()
                .title("Tuyển dụng từ bản nháp")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();

        AppException ex = assertThrows(AppException.class, () -> 
                jobRequestService.publishJobPosting(draftJobRequest.getId(), dto, recruiter.getUsername())
        );

        assertTrue(ex.getMessage().contains("Đã duyệt"));
    }

    /**
     * KN-254: Kế thừa và chỉnh sửa nội dung mô tả công việc theo ngôn ngữ tuyển dụng
     */
    @Test
    @DisplayName("KN-254: Kế thừa và cho phép sửa đổi mô tả công việc, yêu cầu ứng viên phù hợp ngôn ngữ tuyển dụng")
    void testKN254_InheritAndCustomizeJobContent() {
        String customTitle = "🔥 HOT: Senior Java Software Engineer - Thu Nhập Đột Phá";
        String recruitmentDescription = "Bạn đam mê thử thách và muốn xây dựng hệ thống quy mô hàng triệu người dùng? Gia nhập ngay!";
        String recruitmentRequirements = "• Thành thạo Java 17+, Spring Boot, Microservices\n• Giao tiếp tự tin, tư duy kiến trúc tốt";

        PublishJobPostingDto dto = PublishJobPostingDto.builder()
                .title(customTitle)
                .description(recruitmentDescription)
                .requirements(recruitmentRequirements)
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(25))
                .isSalaryVisible(true)
                .build();

        JobRequestDto published = jobRequestService.publishJobPosting(approvedJobRequest.getId(), dto, recruiter.getUsername());

        assertEquals(customTitle, published.getTitle());
        assertEquals(recruitmentDescription, published.getDescription());
        assertEquals(recruitmentRequirements, published.getRequirements());
    }

    /**
     * KN-255: Khai báo thông tin hiển thị của tin tuyển dụng
     */
    @Test
    @DisplayName("KN-255: Khai báo đầy đủ địa điểm, hình thức làm việc, hạn nhận hồ sơ và tùy chọn hiển thị mức lương")
    void testKN255_DeclareDisplayAttributesAndSalaryVisibility() {
        LocalDate deadline = LocalDate.now().plusDays(20);

        // Trường hợp 1: Ẩn mức lương cụ thể (isSalaryVisible = false)
        PublishJobPostingDto dtoHiddenSalary = PublishJobPostingDto.builder()
                .title("Tuyển dụng Kỹ Sư Cao Cấp (Lương Thỏa thuận)")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(deadline)
                .isSalaryVisible(false)
                .build();

        JobRequestDto publishedHidden = jobRequestService.publishJobPosting(approvedJobRequest.getId(), dtoHiddenSalary, recruiter.getUsername());

        assertEquals(locationHanoi.getId(), publishedHidden.getLocationId());
        assertEquals(locationHanoi.getName(), publishedHidden.getLocationName());
        assertEquals(employmentFulltime.getId(), publishedHidden.getEmploymentTypeId());
        assertEquals(employmentFulltime.getName(), publishedHidden.getEmploymentTypeName());
        assertEquals(deadline, publishedHidden.getApplicationDeadline());
        assertFalse(publishedHidden.getIsSalaryVisible());
        assertTrue(publishedHidden.getPublishedBy().equals(recruiter.getFullName()) || publishedHidden.getPublishedBy().equals(recruiter.getUsername()));
        assertNotNull(publishedHidden.getPublishedAt());
    }

    /**
     * KN-256: Kiểm tra hợp lệ dữ liệu trước khi đăng tin (Validation)
     */
    @Test
    @DisplayName("KN-256: Báo lỗi khi hạn nhận hồ sơ ở trong quá khứ hoặc tiêu đề để trống")
    void testKN256_ValidateDataBeforePublishing() {
        // 1. Hạn nhận hồ sơ trong quá khứ
        PublishJobPostingDto pastDeadlineDto = PublishJobPostingDto.builder()
                .title("Tiêu đề hợp lệ")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().minusDays(1))
                .build();

        AppException exDeadline = assertThrows(AppException.class, () ->
                jobRequestService.publishJobPosting(approvedJobRequest.getId(), pastDeadlineDto, recruiter.getUsername())
        );
        assertTrue(exDeadline.getMessage().contains("quá khứ"));

        // 2. Tiêu đề rỗng
        PublishJobPostingDto emptyTitleDto = PublishJobPostingDto.builder()
                .title("   ")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(10))
                .build();

        AppException exTitle = assertThrows(AppException.class, () ->
                jobRequestService.publishJobPosting(approvedJobRequest.getId(), emptyTitleDto, recruiter.getUsername())
        );
        assertTrue(exTitle.getMessage().toLowerCase().contains("tiêu đề"));
    }

    /**
     * KN-257: Hiển thị tin tuyển dụng trên cổng ứng tuyển công khai
     */
    @Test
    @DisplayName("KN-257: Tin tuyển dụng sau khi đăng xuất hiện chính xác trên Cổng việc làm công khai")
    void testKN257_DisplayOnPublicCareerPortal() {
        PublishJobPostingDto dto = PublishJobPostingDto.builder()
                .title("Cơ hội nghề nghiệp: Senior Java Engineer")
                .description("Mô tả công khai cho ứng viên")
                .requirements("Yêu cầu công khai")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(40))
                .isSalaryVisible(true)
                .build();

        jobRequestService.publishJobPosting(approvedJobRequest.getId(), dto, recruiter.getUsername());

        // Kiểm tra qua CareerPortalService
        List<JobRequestDto> publicJobs = careerPortalService.getPublicJobs();
        assertNotNull(publicJobs);

        JobRequestDto publicJob = publicJobs.stream()
                .filter(j -> j.getId().equals(approvedJobRequest.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(publicJob, "Tin tuyển dụng phải hiển thị trên Cổng việc làm công khai");
        assertEquals("OPEN", publicJob.getStatus());
        assertEquals("Cơ hội nghề nghiệp: Senior Java Engineer", publicJob.getTitle());
        assertEquals(locationHanoi.getName(), publicJob.getLocationName());
        assertEquals(employmentFulltime.getName(), publicJob.getEmploymentTypeName());
        assertTrue(publicJob.getIsSalaryVisible());
    }

    /**
     * KN-258: Quản lý trạng thái đăng tin theo trạng thái yêu cầu tuyển dụng
     */
    @Test
    @DisplayName("KN-258: Quản lý vòng đời trạng thái: APPROVED -> OPEN, cho phép cập nhật, và gỡ tin khi yêu cầu đóng")
    void testKN258_LifecycleAndStatusSynchronization() {
        // 1. Xuất bản tin: APPROVED -> OPEN
        PublishJobPostingDto dto = PublishJobPostingDto.builder()
                .title("Tin tuyển dụng ban đầu")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(30))
                .isSalaryVisible(true)
                .build();

        JobRequestDto published = jobRequestService.publishJobPosting(approvedJobRequest.getId(), dto, recruiter.getUsername());
        assertEquals("OPEN", published.getStatus());

        // 2. Chỉnh sửa tin khi đang ở trạng thái OPEN
        PublishJobPostingDto updateDto = PublishJobPostingDto.builder()
                .title("Tin tuyển dụng đã cập nhật thông tin mới")
                .locationId(locationHanoi.getId())
                .employmentTypeId(employmentFulltime.getId())
                .applicationDeadline(LocalDate.now().plusDays(45))
                .isSalaryVisible(false)
                .build();

        JobRequestDto updated = jobRequestService.publishJobPosting(approvedJobRequest.getId(), updateDto, recruiter.getUsername());
        assertEquals("OPEN", updated.getStatus());
        assertEquals("Tin tuyển dụng đã cập nhật thông tin mới", updated.getTitle());
        assertFalse(updated.getIsSalaryVisible());

        // 3. Khi yêu cầu bị đóng (FILLED / PAUSED / CANCELLED): Tự động gỡ khỏi Cổng ứng tuyển ngoài
        CloseJobRequestDto closeDto = CloseJobRequestDto.builder()
                .closingReason("FILLED")
                .closingNote("Đã tuyển đủ 2 nhân sự theo chỉ tiêu headcount")
                .confirmCloseWithPendingCandidates(true)
                .build();

        jobRequestService.closeJobRequest(approvedJobRequest.getId(), closeDto, recruiter.getUsername());

        // Kiểm tra Cổng việc làm công khai -> Tin phải tự động gỡ bỏ (không còn OPEN/APPROVED)
        List<JobRequestDto> publicJobsAfterClose = careerPortalService.getPublicJobs();
        boolean stillVisible = publicJobsAfterClose.stream()
                .anyMatch(j -> j.getId().equals(approvedJobRequest.getId()) && "OPEN".equalsIgnoreCase(j.getStatus()));

        assertFalse(stillVisible, "Tin tuyển dụng phải tự động được gỡ bỏ khỏi Cổng việc làm khi yêu cầu đã đóng");
    }
}

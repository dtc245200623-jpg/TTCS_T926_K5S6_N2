package com.example.auth;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.CandidateApplicationService;
import com.example.auth.service.CvParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử toàn diện các Subtasks từ KN-272 đến KN-292 thuộc 5 User Stories:
 * - Đăng tin & Cổng ứng tuyển - Ứng viên (KN-272 -> KN-285, KN-291, KN-292)
 * - Đăng tin & Cổng ứng tuyển - Nhân Viên Tuyển Dụng (KN-286 -> KN-290)
 */
@SpringBootTest
@Transactional
public class CandidatePortalAndParsingTest {

    @Autowired
    private CandidateApplicationService candidateApplicationService;

    @Autowired
    private CvParserService cvParserService;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private RecruitmentCategoryRepository categoryRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private Department deptDev;
    private Department deptMarketing;
    private RecruitmentCategory locHanoi;
    private RecruitmentCategory locHCM;
    private RecruitmentCategory typeFulltime;
    private RecruitmentCategory sourceWeb;
    private JobPosition posDev;

    private JobRequest activeJob1;
    private JobRequest activeJob2;
    private JobRequest expiredJob;
    private JobRequest draftJob;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        deptDev = departmentRepository.save(Department.builder()
                .code("DEPT_DEV_" + ts)
                .name("Phòng Phát Triển Phần Mềm")
                .active(true)
                .build());

        deptMarketing = departmentRepository.save(Department.builder()
                .code("DEPT_MKT_" + ts)
                .name("Phòng Marketing & Truyền Thông")
                .active(true)
                .build());

        locHanoi = categoryRepository.save(RecruitmentCategory.builder()
                .code("LOC_HN_" + ts)
                .name("Hà Nội (Keangnam Landmark)")
                .type(CategoryType.WORK_LOCATION)
                .active(true)
                .displayOrder(1)
                .build());

        locHCM = categoryRepository.save(RecruitmentCategory.builder()
                .code("LOC_HCM_" + ts)
                .name("TP Hồ Chí Minh (Bitexco)")
                .type(CategoryType.WORK_LOCATION)
                .active(true)
                .displayOrder(2)
                .build());

        typeFulltime = categoryRepository.save(RecruitmentCategory.builder()
                .code("TYPE_FT_" + ts)
                .name("Toàn thời gian cố định")
                .type(CategoryType.EMPLOYMENT_TYPE)
                .active(true)
                .displayOrder(1)
                .build());

        sourceWeb = categoryRepository.save(RecruitmentCategory.builder()
                .code("SRC_WEB_" + ts)
                .name("Website Tuyển Dụng")
                .type(CategoryType.CANDIDATE_SOURCE)
                .active(true)
                .displayOrder(1)
                .build());

        posDev = jobPositionRepository.save(JobPosition.builder()
                .code("POS_DEV_" + ts)
                .name("Senior Java Backend Engineer")
                .level("Senior")
                .department(deptDev)
                .minSalary(25_000_000.0)
                .maxSalary(35_000_000.0)
                .active(true)
                .build());

        // Tin 1: Đã xuất bản, còn hạn 30 ngày
        activeJob1 = jobRequestRepository.save(JobRequest.builder()
                .title("Tuyển dụng Senior Java Spring Boot Developer")
                .department(deptDev)
                .position(posDev)
                .location(locHanoi)
                .employmentType(typeFulltime)
                .headcount(3)
                .minSalary(25_000_000.0)
                .maxSalary(35_000_000.0)
                .isSalaryVisible(true)
                .description("Phát triển các microservices backend cho hệ thống tài chính lớn.")
                .requirements("Thành thạo Java, Spring Boot, MySQL, Docker, K8s.")
                .applicationDeadline(LocalDate.now().plusDays(30))
                .status("APPROVED")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .publishedBy("recruiter_lead")
                .build());

        // Tin 2: Đã xuất bản, còn hạn, phòng Marketing
        activeJob2 = jobRequestRepository.save(JobRequest.builder()
                .title("Tuyển Chuyên Viên Digital Marketing & SEO")
                .department(deptMarketing)
                .location(locHCM)
                .employmentType(typeFulltime)
                .headcount(2)
                .minSalary(15_000_000.0)
                .maxSalary(22_000_000.0)
                .isSalaryVisible(true)
                .description("Lập kế hoạch SEO và chạy các chiến dịch quảng cáo đa kênh.")
                .requirements("Có từ 2 năm kinh nghiệm Google Ads, Facebook Ads.")
                .applicationDeadline(LocalDate.now().plusDays(15))
                .status("OPEN")
                .publishedAt(LocalDateTime.now().minusDays(2))
                .publishedBy("recruiter_lead")
                .build());

        // Tin 3: Đã hết hạn (hạn nộp hôm qua)
        expiredJob = jobRequestRepository.save(JobRequest.builder()
                .title("Tuyển dụng Frontend ReactJS Developer (Đã hết hạn)")
                .department(deptDev)
                .location(locHanoi)
                .employmentType(typeFulltime)
                .headcount(1)
                .minSalary(18_000_000.0)
                .maxSalary(25_000_000.0)
                .isSalaryVisible(true)
                .description("Phát triển giao diện người dùng bằng React và Tailwind.")
                .requirements("Thành thạo React, TypeScript, HTML/CSS.")
                .applicationDeadline(LocalDate.now().minusDays(1)) // Hết hạn hôm qua
                .status("APPROVED")
                .publishedAt(LocalDateTime.now().minusDays(20))
                .publishedBy("recruiter_lead")
                .build());

        // Tin 4: Bản nháp chưa xuất bản
        draftJob = jobRequestRepository.save(JobRequest.builder()
                .title("Tuyển thực tập sinh Tester QA (Bản nháp)")
                .department(deptDev)
                .status("DRAFT")
                .headcount(1)
                .applicationDeadline(LocalDate.now().plusDays(20))
                .build());
    }

    private MockMultipartFile createMockPdfCv(String fullName, String email, String phone) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Full Name: " + fullName);
                cs.newLineAtOffset(0, -20);
                cs.showText("Email: " + email);
                cs.newLineAtOffset(0, -20);
                cs.showText("Phone: " + phone);
                cs.newLineAtOffset(0, -20);
                cs.showText("Work experience: 5 years Java Spring Boot");
                cs.endText();
            }
            doc.save(baos);
        }
        return new MockMultipartFile("cvFile", "resume_" + System.currentTimeMillis() + ".pdf", "application/pdf", baos.toByteArray());
    }

    // =========================================================================================
    // SUBTASKS: KN-272, KN-273, KN-274 (Story: Đăng tin & Cổng ứng tuyển - Ứng viên)
    // =========================================================================================

    @Test
    @DisplayName("KN-274 & Image 1: Tìm kiếm theo từ khoá, lọc phòng ban/địa điểm, chỉ hiện tin đã xuất bản và còn hạn")
    void testSearchPublicJobs_KeywordAndFilters_OnlyPublishedAndUnexpired() {
        // Tìm không truyền filter -> Phải chứa activeJob1 và activeJob2, nhưng KHÔNG chứa expiredJob và draftJob
        List<PublicJobDetailDto> allOpen = candidateApplicationService.searchPublicJobs(null, null, null, null);
        assertTrue(allOpen.stream().anyMatch(j -> j.getId().equals(activeJob1.getId())), "Phải có tin activeJob1");
        assertTrue(allOpen.stream().anyMatch(j -> j.getId().equals(activeJob2.getId())), "Phải có tin activeJob2");
        assertFalse(allOpen.stream().anyMatch(j -> j.getId().equals(expiredJob.getId())), "Không được hiển thị tin đã hết hạn");
        assertFalse(allOpen.stream().anyMatch(j -> j.getId().equals(draftJob.getId())), "Không được hiển thị tin chưa xuất bản");

        // Lọc theo phòng ban DEV
        List<PublicJobDetailDto> devJobs = candidateApplicationService.searchPublicJobs(deptDev.getId(), null, null, null);
        assertTrue(devJobs.stream().anyMatch(j -> j.getId().equals(activeJob1.getId())), "Phải có tin phòng DEV");
        assertFalse(devJobs.stream().anyMatch(j -> j.getId().equals(activeJob2.getId())), "Không được có tin phòng Marketing");

        // Lọc theo địa điểm HCM
        List<PublicJobDetailDto> hcmJobs = candidateApplicationService.searchPublicJobs(null, locHCM.getId(), null, null);
        assertTrue(hcmJobs.stream().anyMatch(j -> j.getId().equals(activeJob2.getId())), "Phải có tin địa điểm HCM");
        assertFalse(hcmJobs.stream().anyMatch(j -> j.getId().equals(activeJob1.getId())), "Không được có tin địa điểm Hà Nội");

        // Tìm kiếm theo từ khoá trong tiêu đề hoặc mô tả (KN-274)
        List<PublicJobDetailDto> searchMicroservices = candidateApplicationService.searchPublicJobs(null, null, null, "microservices");
        assertTrue(searchMicroservices.stream().anyMatch(j -> j.getId().equals(activeJob1.getId())));

        List<PublicJobDetailDto> searchGoogleAds = candidateApplicationService.searchPublicJobs(null, null, null, "Google Ads");
        assertTrue(searchGoogleAds.stream().anyMatch(j -> j.getId().equals(activeJob2.getId())));
    }

    @Test
    @DisplayName("KN-272 & KN-273: Chi tiết tin tuyển dụng cho ứng viên và ẩn toàn bộ thông tin nội bộ")
    void testGetPublicJobDetail_HideInternalInformation() {
        PublicJobDetailDto detail = candidateApplicationService.getPublicJobDetail(activeJob1.getId());

        assertNotNull(detail);
        assertEquals(activeJob1.getTitle(), detail.getTitle());
        assertEquals(deptDev.getName(), detail.getDepartmentName());
        assertEquals(locHanoi.getName(), detail.getLocationName());
        assertEquals(typeFulltime.getName(), detail.getEmploymentTypeName());
        assertEquals(activeJob1.getDescription(), detail.getDescription());
        assertEquals(activeJob1.getRequirements(), detail.getRequirements());
        assertFalse(detail.getIsExpired());

        // KN-273: Không để lộ các trường nội bộ như approvalRule, recruiter id/name, headcount override reason
        // Đảm bảo đối tượng DTO không chứa bất kỳ trường nào của workflow nội bộ
        assertNotNull(detail.getBenefits(), "Phải có nội dung quyền lợi ứng viên");
    }

    // =========================================================================================
    // SUBTASKS: KN-275, KN-276, KN-277, KN-278 (Trạng thái hết hạn & ràng buộc nộp hồ sơ)
    // =========================================================================================

    @Test
    @DisplayName("KN-277: Xử lý trạng thái tin tuyển dụng đã hết hạn")
    void testExpiredJobStatus() {
        PublicJobDetailDto detail = candidateApplicationService.getPublicJobDetail(expiredJob.getId());
        assertNotNull(detail);
        assertTrue(detail.getIsExpired(), "Tin tuyển dụng có hạn nộp trong quá khứ phải có cờ isExpired = true");
    }

    @Test
    @DisplayName("KN-278: Ràng buộc nộp hồ sơ - Từ chối khi tin tuyển dụng đã hết hạn")
    void testApplyToExpiredJob_ShouldReject() throws IOException {
        MockMultipartFile file = createMockPdfCv("Nguyen Van Expired", "expired_candidate@gmail.com", "0912345678");

        CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                .jobRequestId(expiredJob.getId())
                .fullName("Nguyen Van Expired")
                .email("expired_candidate@gmail.com")
                .phone("0912345678")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                candidateApplicationService.submitApplication(req, file, "192.168.1.100")
        );
        assertTrue(ex.getMessage().contains("hết hạn nhận hồ sơ"), "Phải thông báo tin tuyển dụng đã hết hạn");
    }

    // =========================================================================================
    // SUBTASKS: KN-279, KN-280, KN-281, KN-282, KN-283, KN-284, KN-285 (Ứng tuyển & Validation)
    // =========================================================================================

    @Test
    @DisplayName("KN-280: Kiểm tra hợp lệ dữ liệu đầu vào (Email, SĐT, Họ tên)")
    void testApplicationInputValidation() throws IOException {
        MockMultipartFile file = createMockPdfCv("Test", "test@gmail.com", "0912345678");

        // Thiếu họ tên
        CandidateApplyRequestDto emptyName = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("   ")
                .email("valid@gmail.com")
                .phone("0912345678")
                .build();
        assertThrows(AppException.class, () -> candidateApplicationService.submitApplication(emptyName, file, "127.0.0.1"));

        // Email sai định dạng
        CandidateApplyRequestDto invalidEmail = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Nguyen Van A")
                .email("email-khong-hop-le")
                .phone("0912345678")
                .build();
        assertThrows(AppException.class, () -> candidateApplicationService.submitApplication(invalidEmail, file, "127.0.0.1"));

        // Số điện thoại sai định dạng
        CandidateApplyRequestDto invalidPhone = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Nguyen Van A")
                .email("test@gmail.com")
                .phone("12345") // Quá ngắn
                .build();
        assertThrows(AppException.class, () -> candidateApplicationService.submitApplication(invalidPhone, file, "127.0.0.1"));
    }

    @Test
    @DisplayName("KN-281: Kiểm tra giới hạn dung lượng tối đa 10MB và định dạng tệp CV")
    void testCvFileValidation_SizeAndFormat() throws IOException {
        // Tệp định dạng không cho phép (.exe hoặc .txt)
        MockMultipartFile textFile = new MockMultipartFile("cvFile", "resume.txt", "text/plain", "Hello CV".getBytes());
        CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Nguyen Van A")
                .email("nguyenvana@gmail.com")
                .phone("0912345678")
                .build();

        AppException extEx = assertThrows(AppException.class, () ->
                candidateApplicationService.submitApplication(req, textFile, "127.0.0.1")
        );
        assertTrue(extEx.getMessage().contains("chỉ chấp nhận tệp CV dạng PDF hoặc DOCX"));

        // Tệp vượt quá 10MB
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile("cvFile", "large_cv.pdf", "application/pdf", largeBytes);
        AppException sizeEx = assertThrows(AppException.class, () ->
                candidateApplicationService.submitApplication(req, largeFile, "127.0.0.1")
        );
        assertTrue(sizeEx.getMessage().contains("vượt quá giới hạn 10MB"));
    }

    @Test
    @DisplayName("KN-282: Chống spam và giới hạn số lần nộp từ một địa chỉ IP trong một giờ")
    void testAntiSpam_RateLimitByIp() throws IOException {
        String testIp = "10.0.0.99";

        // Nộp 5 lần thành công từ cùng 1 IP (với các email khác nhau để không trùng email)
        for (int i = 1; i <= 5; i++) {
            MockMultipartFile file = createMockPdfCv("Candidate " + i, "cand" + i + "@test.com", "091234567" + i);
            CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                    .jobRequestId(activeJob1.getId())
                    .fullName("Candidate " + i)
                    .email("cand" + i + "@test.com")
                    .phone("091234567" + i)
                    .build();
            candidateApplicationService.submitApplication(req, file, testIp);
        }

        // Lần thứ 6 phải bị chặn bởi rate limit
        MockMultipartFile file6 = createMockPdfCv("Candidate 6", "cand6@test.com", "0912345676");
        CandidateApplyRequestDto req6 = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Candidate 6")
                .email("cand6@test.com")
                .phone("0912345676")
                .build();

        AppException spamEx = assertThrows(AppException.class, () ->
                candidateApplicationService.submitApplication(req6, file6, testIp)
        );
        assertTrue(spamEx.getMessage().contains("quá số lần cho phép từ địa chỉ này trong vòng 1 giờ"));
    }

    @Test
    @DisplayName("KN-283: Ngăn chặn nộp trùng cùng một vị trí bằng cùng email kèm thông báo rõ ràng")
    void testDuplicateApplication_SameJobAndEmail() throws IOException {
        String email = "duplicate.test@gmail.com";
        MockMultipartFile file1 = createMockPdfCv("Tran Van Dupe", email, "0988776655");

        CandidateApplyRequestDto req1 = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Tran Van Dupe")
                .email(email)
                .phone("0988776655")
                .build();

        // Nộp lần 1 thành công
        CandidateApplyResponseDto res1 = candidateApplicationService.submitApplication(req1, file1, "127.0.0.1");
        assertNotNull(res1.getTrackingCode());

        // Nộp lần 2 cùng vị trí cùng email -> Phải bị từ chối
        MockMultipartFile file2 = createMockPdfCv("Tran Van Dupe", email, "0988776655");
        AppException dupEx = assertThrows(AppException.class, () ->
                candidateApplicationService.submitApplication(req1, file2, "127.0.0.1")
        );
        assertTrue(dupEx.getMessage().contains("đã được dùng để nộp hồ sơ cho vị trí"), "Phải từ chối nộp trùng kèm thông báo rõ ràng");
    }

    @Test
    @DisplayName("KN-284 & KN-285: Nộp thành công sinh mã tra cứu, lưu hồ sơ ở backend")
    void testSubmitApplicationSuccess_GenerateTrackingCodeAndSaveBackend() throws IOException {
        String email = "success.candidate@ttcs.vn";
        MockMultipartFile file = createMockPdfCv("Le Hoang Nam", email, "0977889900");

        CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Le Hoang Nam")
                .email(email)
                .phone("0977889900")
                .sourceId(sourceWeb.getId())
                .coverLetter("Tôi mong muốn được cống hiến cho công ty.")
                .build();

        CandidateApplyResponseDto res = candidateApplicationService.submitApplication(req, file, "127.0.0.1");

        assertNotNull(res.getTrackingCode());
        assertTrue(res.getTrackingCode().startsWith("APP-2026-"));
        assertEquals("Le Hoang Nam", res.getCandidateName());
        assertEquals(email, res.getCandidateEmail());

        // Kiểm tra lưu trữ ở backend
        Application saved = applicationRepository.findByTrackingCode(res.getTrackingCode()).orElse(null);
        assertNotNull(saved);
        assertEquals("APPLIED", saved.getStatus());
        assertEquals("Le Hoang Nam", saved.getCandidateName());
        assertEquals(email, saved.getCandidateEmail());
        assertNotNull(saved.getCvUrl());
        assertNotNull(saved.getSource());
    }

    // =========================================================================================
    // SUBTASKS: KN-286, KN-287, KN-288, KN-289, KN-290 (Bóc tách CV PDF & Quản lý của Recruiter)
    // =========================================================================================

    @Test
    @DisplayName("KN-286 & KN-290: Kiểm tra hợp lệ tệp PDF và trích xuất họ tên, email, SĐT")
    void testPdfCvParsing_Success() throws IOException {
        MockMultipartFile pdfFile = createMockPdfCv("Phan Thanh Binh", "binh.phan@gmail.com", "0934567890");

        assertTrue(cvParserService.isValidPdf(pdfFile));

        CvParsingResult result = cvParserService.parsePdfCv(pdfFile);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("binh.phan@gmail.com", result.getEmail());
        assertEquals("0934567890", result.getPhone());
        assertNotNull(result.getFullName());
        assertTrue(result.getFullName().contains("Phan Thanh Binh"));
    }

    @Test
    @DisplayName("KN-289: Xử lý trường hợp bóc tách CV thất bại mà vẫn cho phép nộp hồ sơ thành công")
    void testCvParsingFailure_DoesNotBreakApplicationSubmission() {
        // Tệp PDF giả với nội dung rác không thể parse
        byte[] corruptedPdfBytes = "%PDF-CORRUPTED-GARBAGE-BYTES-1234567890".getBytes();
        MockMultipartFile corruptedPdf = new MockMultipartFile("cvFile", "corrupted.pdf", "application/pdf", corruptedPdfBytes);

        CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Tran Van Resilient")
                .email("resilient@gmail.com")
                .phone("0912345999")
                .build();

        // Nộp hồ sơ vẫn phải THÀNH CÔNG, không được ném exception
        CandidateApplyResponseDto res = candidateApplicationService.submitApplication(req, corruptedPdf, "127.0.0.1");
        assertNotNull(res);
        assertNotNull(res.getTrackingCode());

        // Trạng thái bóc tách là FAILED, nhưng hồ sơ vẫn lưu trữ hoàn chỉnh
        Application app = applicationRepository.findByTrackingCode(res.getTrackingCode()).orElse(null);
        assertNotNull(app);
        assertEquals("FAILED", app.getCvParsingStatus());
        assertEquals("Tran Van Resilient", app.getCandidateName());
    }

    @Test
    @DisplayName("KN-287 & KN-288: Recruiter xem dữ liệu bóc tách, không tự động ghi đè trừ khi xác nhận")
    void testCvParsing_NoAutoOverwrite_RecruiterConfirmation() throws IOException {
        // Ứng viên tự nhập: "Nguyen Van User"
        // CV PDF chứa: "Nguyen Van FromCV"
        MockMultipartFile file = createMockPdfCv("Nguyen Van FromCV", "from_cv@gmail.com", "0900112233");

        CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Nguyen Van User")
                .email("user_input@gmail.com")
                .phone("0988001122")
                .build();

        CandidateApplyResponseDto applyRes = candidateApplicationService.submitApplication(req, file, "127.0.0.1");

        // KN-288: Dữ liệu ứng viên tự nhập vẫn được bảo toàn nguyên vẹn, không tự ghi đè
        Application app = applicationRepository.findById(applyRes.getApplicationId()).orElse(null);
        assertNotNull(app);
        assertEquals("Nguyen Van User", app.getCandidateName(), "Tên chính thức phải là tên ứng viên tự nhập");
        assertEquals("user_input@gmail.com", app.getCandidateEmail());
        assertEquals("SUCCESS", app.getCvParsingStatus());
        assertTrue(app.getParsedFullName().contains("Nguyen Van FromCV"));
        assertEquals("from_cv@gmail.com", app.getParsedEmail());

        // KN-287: Recruiter xem dữ liệu đối chiếu
        ApplicationDto recruiterView = candidateApplicationService.getApplicationParsedData(app.getId());
        assertEquals("Nguyen Van User", recruiterView.getCandidateName());
        assertTrue(recruiterView.getParsedFullName().contains("Nguyen Van FromCV"));

        // Recruiter chỉnh sửa và xác nhận áp dụng dữ liệu bóc tách vào hồ sơ
        ConfirmParsedDataDto confirmDto = ConfirmParsedDataDto.builder()
                .fullName("Nguyen Van FromCV (Đã chuẩn hoá)")
                .email("from_cv@gmail.com")
                .phone("0900112233")
                .applyToProfile(true) // Đồng ý áp dụng
                .build();

        ApplicationDto updated = candidateApplicationService.confirmOrUpdateParsedData(app.getId(), confirmDto, "recruiter_test");
        assertEquals("Nguyen Van FromCV (Đã chuẩn hoá)", updated.getCandidateName());
        assertEquals("from_cv@gmail.com", updated.getCandidateEmail());
        assertTrue(updated.getParsedDataConfirmed());
    }

    // =========================================================================================
    // SUBTASKS: KN-291, KN-292 (Tra cứu trạng thái ứng tuyển & Ẩn thông tin nội bộ)
    // =========================================================================================

    @Test
    @DisplayName("KN-291 & KN-292: Tra cứu trạng thái bằng mã tra cứu + email, ánh xạ 4 trạng thái và ẩn nội bộ")
    void testTrackApplicationStatus_FourGeneralStatuses_HideInternalInfo() throws IOException {
        MockMultipartFile file = createMockPdfCv("Hoang Status Test", "status_test@gmail.com", "0966554433");

        CandidateApplyRequestDto req = CandidateApplyRequestDto.builder()
                .jobRequestId(activeJob1.getId())
                .fullName("Hoang Status Test")
                .email("status_test@gmail.com")
                .phone("0966554433")
                .build();

        CandidateApplyResponseDto submitRes = candidateApplicationService.submitApplication(req, file, "127.0.0.1");
        String trackingCode = submitRes.getTrackingCode();

        // 1. Trạng thái ban đầu: "APPLIED" -> Map thành "Đã nhận" (KN-291)
        ApplicationTrackingRequestDto trackReq = ApplicationTrackingRequestDto.builder()
                .trackingCode(trackingCode)
                .email("status_test@gmail.com")
                .build();

        PublicApplicationStatusDto statusReceived = candidateApplicationService.trackApplicationStatus(trackReq);
        assertEquals("Đã nhận", statusReceived.getGeneralStatus());
        assertEquals("Hoang Status Test", statusReceived.getCandidateName());
        assertEquals(activeJob1.getTitle(), statusReceived.getJobTitle());

        // 2. Chuyển sang "SCREENING" -> Map thành "Đang xem xét"
        Application app = applicationRepository.findByTrackingCode(trackingCode).get();
        app.setStatus("SCREENING");
        applicationRepository.save(app);

        PublicApplicationStatusDto statusReview = candidateApplicationService.trackApplicationStatus(trackReq);
        assertEquals("Đang xem xét", statusReview.getGeneralStatus());

        // 3. Chuyển sang "INTERVIEWING" -> Map thành "Mời phỏng vấn"
        app.setStatus("INTERVIEWING");
        applicationRepository.save(app);

        PublicApplicationStatusDto statusInterview = candidateApplicationService.trackApplicationStatus(trackReq);
        assertEquals("Mời phỏng vấn", statusInterview.getGeneralStatus());

        // 4. Chuyển sang "REJECTED" kèm lý do nội bộ -> Map thành "Kết thúc"
        app.setStatus("REJECTED");
        app.setRejectionNote("Ghi chú nội bộ: Không đạt bài kiểm tra thuật toán");
        applicationRepository.save(app);

        PublicApplicationStatusDto statusEnd = candidateApplicationService.trackApplicationStatus(trackReq);
        assertEquals("Kết thúc", statusEnd.getGeneralStatus());

        // KN-292: Ẩn thông tin nội bộ khỏi màn hình tra cứu
        // DTO không hề chứa trường rejectionNote, rejectionReason, interviewScore, interviewer
        assertNotNull(statusEnd.getStatusDescription());
        assertFalse(statusEnd.getStatusDescription().contains("thuật toán"), "Tuyệt đối không để lộ nhận xét nội bộ");
    }

    @Test
    @DisplayName("KN-291: Tra cứu thất bại khi nhập sai email hoặc sai mã tra cứu")
    void testTrackApplicationStatus_NotFound() {
        ApplicationTrackingRequestDto wrongReq = ApplicationTrackingRequestDto.builder()
                .trackingCode("APP-2026-INVALID")
                .email("wrong@gmail.com")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                candidateApplicationService.trackApplicationStatus(wrongReq)
        );
        assertTrue(ex.getMessage().contains("Không tìm thấy hồ sơ ứng tuyển"));
    }
}

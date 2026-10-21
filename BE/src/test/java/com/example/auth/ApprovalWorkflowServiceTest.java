package com.example.auth;

import com.example.auth.dto.ApprovalRuleDto;
import com.example.auth.dto.ApprovalRuleStepDto;
import com.example.auth.dto.JobRequestApprovalStepDto;
import com.example.auth.dto.JobRequestDto;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.ApprovalWorkflowService;
import com.example.auth.service.JobRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ApprovalWorkflowServiceTest {

    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;

    @Autowired
    private JobRequestService jobRequestService;

    @Autowired
    private ApprovalRuleRepository approvalRuleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    private Department testDept;
    private JobPosition testPosition;
    private User testHrManager;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        Set<String> hrRoles = new HashSet<>();
        hrRoles.add("ROLE_HR_MANAGER");

        testHrManager = userRepository.findByUsername("test_hr_manager_workflow").orElseGet(() -> {
            User u = new User();
            u.setUsername("test_hr_manager_workflow");
            u.setPassword("encoded_pass");
            u.setFullName("Trưởng Phòng Nhân Sự Test");
            u.setEmail("hr_manager_workflow@example.com");
            u.setRoles(hrRoles);
            return userRepository.save(u);
        });

        Set<String> adminRoles = new HashSet<>();
        adminRoles.add("ROLE_ADMIN");

        testAdmin = userRepository.findByUsername("test_admin_workflow").orElseGet(() -> {
            User u = new User();
            u.setUsername("test_admin_workflow");
            u.setPassword("encoded_pass");
            u.setFullName("Ban Giám Đốc Admin Test");
            u.setEmail("admin_workflow@example.com");
            u.setRoles(adminRoles);
            return userRepository.save(u);
        });

        testDept = departmentRepository.save(Department.builder()
                .code("DEPT_WF_" + System.currentTimeMillis())
                .name("Phòng Công Nghệ Thông Tin")
                .active(true)
                .build());

        testPosition = jobPositionRepository.save(JobPosition.builder()
                .code("POS_WF_" + System.currentTimeMillis())
                .name("Kỹ Sư Hệ Thống Cao Cấp")
                .level("Senior")
                .department(testDept)
                .minSalary(20_000_000.0)
                .maxSalary(30_000_000.0)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("KN-220: Khai báo chuỗi cấp duyệt theo phòng ban và theo mức lương đề xuất thành công")
    void testCreateRule_KN220_Success() {
        List<ApprovalRuleStepDto> steps = new ArrayList<>();
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Trưởng phòng Nhân sự thẩm định")
                .approverRole("ROLE_HR_MANAGER")
                .description("Kiểm tra định biên và ngân sách")
                .build());
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(2)
                .stepName("Ban Giám đốc phê duyệt")
                .approverRole("ROLE_ADMIN")
                .description("Phê duyệt cấp cao")
                .build());

        ApprovalRuleDto dto = ApprovalRuleDto.builder()
                .name("Chuỗi duyệt phòng IT - Lương từ 15M đến 35M")
                .departmentId(testDept.getId())
                .minSalary(15_000_000.0)
                .maxSalary(35_000_000.0)
                .budgetLimit(30_000_000.0)
                .autoEscalateOverBudget(true)
                .overBudgetStepName("Tổng Giám đốc phê duyệt vượt hạn mức")
                .overBudgetRole("ROLE_ADMIN")
                .active(true)
                .priority(10)
                .steps(steps)
                .build();

        ApprovalRuleDto created = approvalWorkflowService.createRule(dto, testHrManager.getUsername());

        assertNotNull(created.getId());
        assertEquals("Chuỗi duyệt phòng IT - Lương từ 15M đến 35M", created.getName());
        assertEquals(testDept.getId(), created.getDepartmentId());
        assertEquals(2, created.getSteps().size());
        assertEquals(1, created.getSteps().get(0).getStepOrder());
        assertEquals("ROLE_HR_MANAGER", created.getSteps().get(0).getApproverRole());
        assertEquals(2, created.getSteps().get(1).getStepOrder());
        assertEquals("ROLE_ADMIN", created.getSteps().get(1).getApproverRole());
    }

    @Test
    @DisplayName("KN-222: Cấu hình sai (không có cấp duyệt nào khớp / rỗng cấp duyệt) bị chặn lưu")
    void testCreateRule_KN222_EmptySteps_ThrowsException() {
        ApprovalRuleDto dto = ApprovalRuleDto.builder()
                .name("Quy tắc sai - Không có cấp duyệt nào")
                .departmentId(testDept.getId())
                .steps(Collections.emptyList()) // Rỗng cấp duyệt!
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                approvalWorkflowService.createRule(dto, testHrManager.getUsername())
        );

        assertTrue(ex.getMessage().contains("Phải có ít nhất 1 cấp duyệt"),
                "Phải chặn lưu và thông báo lỗi. Thực tế: " + ex.getMessage());
    }

    @Test
    @DisplayName("KN-222: Cấu hình sai (cấp duyệt chưa phân công người/vai trò duyệt) bị chặn lưu")
    void testCreateRule_KN222_MissingApprover_ThrowsException() {
        List<ApprovalRuleStepDto> steps = new ArrayList<>();
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Bước 1 không gán người")
                .approverRole(null) // Không có vai trò
                .approverUserId(null) // Không có user
                .build());

        ApprovalRuleDto dto = ApprovalRuleDto.builder()
                .name("Quy tắc sai - Cấp duyệt vô chủ")
                .departmentId(testDept.getId())
                .steps(steps)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                approvalWorkflowService.createRule(dto, testHrManager.getUsername())
        );

        assertTrue(ex.getMessage().contains("chưa được phân công người hoặc vai trò"),
                "Phải chặn lưu khi cấp duyệt vô chủ. Thực tế: " + ex.getMessage());
    }

    @Test
    @DisplayName("KN-222: Cấu hình sai (thứ tự cấp duyệt bị trùng lặp) bị chặn lưu")
    void testCreateRule_KN222_DuplicateStepOrder_ThrowsException() {
        List<ApprovalRuleStepDto> steps = new ArrayList<>();
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Bước 1")
                .approverRole("ROLE_HR_MANAGER")
                .build());
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1) // Trùng thứ tự 1!
                .stepName("Bước 1 trùng lặp")
                .approverRole("ROLE_ADMIN")
                .build());

        ApprovalRuleDto dto = ApprovalRuleDto.builder()
                .name("Quy tắc sai - Trùng thứ tự")
                .steps(steps)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                approvalWorkflowService.createRule(dto, testHrManager.getUsername())
        );

        assertTrue(ex.getMessage().contains("trùng lặp"),
                "Phải chặn lưu khi trùng thứ tự bước. Thực tế: " + ex.getMessage());
    }

    @Test
    @DisplayName("KN-222: Cấu hình dải lương sai (minSalary > maxSalary) bị chặn lưu")
    void testCreateRule_KN222_InvalidSalaryRange_ThrowsException() {
        List<ApprovalRuleStepDto> steps = new ArrayList<>();
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Bước 1")
                .approverRole("ROLE_HR_MANAGER")
                .build());

        ApprovalRuleDto dto = ApprovalRuleDto.builder()
                .name("Quy tắc sai - Lương min > max")
                .minSalary(50_000_000.0)
                .maxSalary(20_000_000.0) // min > max
                .steps(steps)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                approvalWorkflowService.createRule(dto, testHrManager.getUsername())
        );

        assertTrue(ex.getMessage().contains("Lương tối thiểu không được lớn hơn lương tối đa"),
                "Phải chặn lưu khi dải lương sai logic. Thực tế: " + ex.getMessage());
    }

    @Test
    @DisplayName("KN-221: Yêu cầu tuyển dụng vượt hạn mức tự động thêm cấp duyệt cao hơn")
    void testSubmitJobRequest_KN221_OverBudget_AutoEscalatesHigherLevel() {
        // Tạo quy tắc chuẩn có 1 bước thường, budgetLimit = 25M, trần vị trí = 30M
        List<ApprovalRuleStepDto> steps = new ArrayList<>();
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Trưởng phòng Nhân sự phê duyệt thông thường")
                .approverRole("ROLE_HR_MANAGER")
                .build());

        ApprovalRuleDto ruleDto = ApprovalRuleDto.builder()
                .name("Quy tắc IT ngân sách tối đa 25M")
                .departmentId(testDept.getId())
                .budgetLimit(25_000_000.0)
                .autoEscalateOverBudget(true)
                .overBudgetStepName("Ban Giám đốc phê duyệt vượt ngân sách (KN-221)")
                .overBudgetRole("ROLE_ADMIN")
                .active(true)
                .priority(50)
                .steps(steps)
                .build();

        approvalWorkflowService.createRule(ruleDto, testHrManager.getUsername());

        // Tạo yêu cầu tuyển dụng với mức lương đề xuất = 35M (Vượt cả ngân sách 25M và trần vị trí 30M)
        JobRequestDto requestDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(1)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(LocalDate.now().plusMonths(1))
                .minSalary(28_000_000.0)
                .maxSalary(35_000_000.0) // Vượt hạn mức 25M và trần 30M
                .salaryExplanation("Ứng viên xuất sắc có chứng chỉ quốc tế cấp cao")
                .description("Tuyển dụng Architect cao cấp")
                .requirements("10+ năm kinh nghiệm kiến trúc hệ thống")
                .build();

        JobRequestDto createdRequest = jobRequestService.createOrDraftJobRequest(requestDto, testHrManager.getUsername(), false);

        // Kiểm tra danh sách bước snapshot đã tạo
        List<JobRequestApprovalStepDto> approvalSteps = approvalWorkflowService.getApprovalStepsForJobRequest(createdRequest.getId());

        // Kỳ vọng: Ban đầu rule có 1 bước, vì vượt hạn mức nên tự động thêm 1 cấp duyệt cao hơn -> Tổng cộng 2 bước!
        assertEquals(2, approvalSteps.size(), "Số bước duyệt phải tự động tăng thêm cấp duyệt cao hơn khi vượt hạn mức");

        JobRequestApprovalStepDto step1 = approvalSteps.get(0);
        assertEquals(1, step1.getStepOrder());
        assertEquals("Trưởng phòng Nhân sự phê duyệt thông thường", step1.getStepName());
        assertEquals("PENDING", step1.getStatus());
        assertFalse(step1.getIsOverBudgetStep());

        JobRequestApprovalStepDto step2Escalated = approvalSteps.get(1);
        assertEquals(2, step2Escalated.getStepOrder());
        assertEquals("Ban Giám đốc phê duyệt vượt ngân sách (KN-221)", step2Escalated.getStepName());
        assertEquals("ROLE_ADMIN", step2Escalated.getApproverRole());
        assertEquals("WAITING", step2Escalated.getStatus());
        assertTrue(step2Escalated.getIsOverBudgetStep(), "Bước này phải được đánh dấu là vượt hạn mức (KN-221)");
    }

    @Test
    @DisplayName("KN-235: Luồng đang chạy không bị ảnh hưởng khi cấu hình quy tắc thay đổi")
    void testRunningWorkflow_KN235_NotAffectedByRuleChange() {
        // 1. Tạo quy tắc ban đầu Rule A gồm 2 bước
        List<ApprovalRuleStepDto> steps = new ArrayList<>();
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Bước 1: Trưởng phòng chuyên môn")
                .approverRole("ROLE_HR_MANAGER")
                .build());
        steps.add(ApprovalRuleStepDto.builder()
                .stepOrder(2)
                .stepName("Bước 2: Giám đốc Khối")
                .approverRole("ROLE_ADMIN")
                .build());

        ApprovalRuleDto ruleDto = ApprovalRuleDto.builder()
                .name("Quy tắc ban đầu Rule A")
                .departmentId(testDept.getId())
                .budgetLimit(50_000_000.0)
                .autoEscalateOverBudget(false)
                .active(true)
                .priority(100)
                .steps(steps)
                .build();

        ApprovalRuleDto createdRule = approvalWorkflowService.createRule(ruleDto, testHrManager.getUsername());

        // 2. Tạo yêu cầu tuyển dụng chạy theo quy tắc ban đầu
        JobRequestDto requestDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(1)
                .recruitmentReason("EXPANSION")
                .neededDate(LocalDate.now().plusMonths(1))
                .minSalary(22_000_000.0)
                .maxSalary(25_000_000.0)
                .description("Tuyển dụng theo quy trình chuẩn ban đầu")
                .requirements("Yêu cầu năng lực")
                .build();

        JobRequestDto jobRequest = jobRequestService.createOrDraftJobRequest(requestDto, testHrManager.getUsername(), false);

        List<JobRequestApprovalStepDto> originalRunningSteps = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        assertEquals(2, originalRunningSteps.size());
        assertEquals("Bước 1: Trưởng phòng chuyên môn", originalRunningSteps.get(0).getStepName());
        assertEquals("Bước 2: Giám đốc Khối", originalRunningSteps.get(1).getStepName());

        // 3. Người quản trị vào thay đổi cấu hình quy tắc (Đổi tên bước, thêm bước thứ 3, đổi hạn mức)
        List<ApprovalRuleStepDto> updatedSteps = new ArrayList<>();
        updatedSteps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Bước mới hoàn toàn: Nhân sự cấp cao")
                .approverRole("ROLE_HR_MANAGER")
                .build());
        updatedSteps.add(ApprovalRuleStepDto.builder()
                .stepOrder(2)
                .stepName("Bước mới: Ban Kiểm Soát")
                .approverRole("ROLE_ADMIN")
                .build());
        updatedSteps.add(ApprovalRuleStepDto.builder()
                .stepOrder(3)
                .stepName("Bước mới: Hội Đồng Quản Trị")
                .approverRole("ROLE_ADMIN")
                .build());

        ApprovalRuleDto updateDto = ApprovalRuleDto.builder()
                .name("Quy tắc đã bị sửa đổi hoàn toàn")
                .departmentId(testDept.getId())
                .budgetLimit(10_000_000.0)
                .active(true)
                .steps(updatedSteps)
                .build();

        approvalWorkflowService.updateRule(createdRule.getId(), updateDto, testHrManager.getUsername());

        // 4. Kiểm tra: Luồng đang chạy của JobRequest trước đó PHẢI KHÔNG BỊ ẢNH HƯỞNG (KN-235)
        List<JobRequestApprovalStepDto> runningStepsAfterRuleModified = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());

        assertEquals(2, runningStepsAfterRuleModified.size(), "Số bước của yêu cầu đang chạy vẫn phải là 2 bước");
        assertEquals("Bước 1: Trưởng phòng chuyên môn", runningStepsAfterRuleModified.get(0).getStepName(), "Tên bước 1 của yêu cầu đang chạy không được bị thay đổi");
        assertEquals("Bước 2: Giám đốc Khối", runningStepsAfterRuleModified.get(1).getStepName(), "Tên bước 2 của yêu cầu đang chạy không được bị thay đổi");
        assertEquals("PENDING", runningStepsAfterRuleModified.get(0).getStatus());
        assertEquals("WAITING", runningStepsAfterRuleModified.get(1).getStatus());
    }

    @Test
    @DisplayName("Quy trình phê duyệt tuần tự qua các cấp: Cấp 1 duyệt -> Cấp 2 chờ duyệt -> Cấp 2 duyệt -> Hoàn tất APPROVED")
    void testProcessApprovalSteps_MultiLevel_Success() {
        // Thiết lập quy tắc 2 bước duyệt cụ thể cho phòng ban test
        List<ApprovalRuleStepDto> ruleSteps = new ArrayList<>();
        ruleSteps.add(ApprovalRuleStepDto.builder()
                .stepOrder(1)
                .stepName("Trưởng phòng Nhân sự thẩm định")
                .approverRole("ROLE_HR_MANAGER")
                .build());
        ruleSteps.add(ApprovalRuleStepDto.builder()
                .stepOrder(2)
                .stepName("Người duyệt / Ban Giám đốc phê duyệt")
                .approverRole("ROLE_ADMIN")
                .build());

        ApprovalRuleDto customRule = ApprovalRuleDto.builder()
                .name("Chuỗi duyệt test đa cấp " + System.currentTimeMillis())
                .departmentId(testDept.getId())
                .budgetLimit(50_000_000.0)
                .autoEscalateOverBudget(false)
                .active(true)
                .priority(1000)
                .steps(ruleSteps)
                .build();
        approvalWorkflowService.createRule(customRule, testHrManager.getUsername());

        // Tạo JobRequest
        JobRequestDto requestDto = JobRequestDto.builder()
                .positionId(testPosition.getId())
                .departmentId(testDept.getId())
                .headcount(1)
                .recruitmentReason("NEW_HEADCOUNT")
                .neededDate(LocalDate.now().plusMonths(1))
                .minSalary(22_000_000.0)
                .maxSalary(25_000_000.0)
                .description("Tuyển dụng vị trí Senior")
                .requirements("Yêu cầu đầy đủ")
                .build();

        JobRequestDto jobRequest = jobRequestService.createOrDraftJobRequest(requestDto, testHrManager.getUsername(), false);

        List<JobRequestApprovalStepDto> steps = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        assertEquals(2, steps.size(), "Cần có đúng 2 bước duyệt");

        JobRequestApprovalStepDto step1 = steps.get(0);
        JobRequestApprovalStepDto step2 = steps.get(1);

        assertEquals("PENDING", step1.getStatus());
        assertEquals("WAITING", step2.getStatus());

        // 1. Phê duyệt bước 1 bởi HR Manager
        JobRequestApprovalStepDto approvedStep1 = approvalWorkflowService.processApprovalStep(
                jobRequest.getId(),
                step1.getId(),
                true,
                "Đồng ý định biên bước 1",
                testHrManager.getUsername()
        );

        assertEquals("APPROVED", approvedStep1.getStatus());

        // Kiểm tra bước 2 đã tự động chuyển sang PENDING
        List<JobRequestApprovalStepDto> stepsAfterStep1 = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        assertEquals("APPROVED", stepsAfterStep1.get(0).getStatus());
        assertEquals("PENDING", stepsAfterStep1.get(1).getStatus(), "Bước 2 phải tự động chuyển sang PENDING sau khi bước 1 duyệt");

        // Trạng thái JobRequest vẫn là PENDING_APPROVAL
        JobRequest currentJob = jobRequestRepository.findById(jobRequest.getId()).orElseThrow();
        assertEquals("PENDING_APPROVAL", currentJob.getStatus());

        // 2. Phê duyệt bước 2 bởi Admin (Ban Giám đốc)
        JobRequestApprovalStepDto approvedStep2 = approvalWorkflowService.processApprovalStep(
                jobRequest.getId(),
                step2.getId(),
                true,
                "Ban Giám đốc phê duyệt chính thức",
                testAdmin.getUsername()
        );

        assertEquals("APPROVED", approvedStep2.getStatus());

        // Kiểm tra trạng thái cuối cùng của JobRequest: APPROVED
        JobRequest finalizedJob = jobRequestRepository.findById(jobRequest.getId()).orElseThrow();
        assertEquals("APPROVED", finalizedJob.getStatus(), "Yêu cầu phải chuyển sang APPROVED sau khi tất cả các cấp duyệt xong");
    }

    @Test
    @DisplayName("KN-236, KN-237, KN-241: Từ chối yêu cầu bắt buộc nhập ý kiến và ghi nhận lịch sử xử lý")
    void testRejectStep_KN236_KN237_KN241() {
        ApprovalRuleDto ruleDto = ApprovalRuleDto.builder()
                .name("Quy trình duyệt test từ chối " + System.currentTimeMillis())
                .departmentId(testDept.getId())
                .active(true)
                .steps(List.of(
                        ApprovalRuleStepDto.builder().stepOrder(1).stepName("Bước 1: HR Manager").approverRole("ROLE_HR_MANAGER").build(),
                        ApprovalRuleStepDto.builder().stepOrder(2).stepName("Bước 2: Giám đốc").approverRole("ROLE_ADMIN").build()
                ))
                .build();
        approvalWorkflowService.createRule(ruleDto, testAdmin.getUsername());

        JobRequestDto requestDto = JobRequestDto.builder()
                .departmentId(testDept.getId())
                .positionId(testPosition.getId())
                .title("Tuyển dụng kiểm thử từ chối")
                .headcount(1)
                .recruitmentReason("REPLACEMENT")
                .neededDate(LocalDate.now().plusDays(30))
                .minSalary(22_000_000.0)
                .maxSalary(28_000_000.0)
                .description("Mô tả đầy đủ")
                .requirements("Yêu cầu đầy đủ")
                .build();

        JobRequestDto jobRequest = jobRequestService.createOrDraftJobRequest(requestDto, testHrManager.getUsername(), false);
        List<JobRequestApprovalStepDto> steps = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        JobRequestApprovalStepDto step1 = steps.get(0);

        // KN-237: Từ chối nhưng không nhập ý kiến phải ném AppException
        AppException ex = assertThrows(AppException.class, () ->
                approvalWorkflowService.processApprovalStep(jobRequest.getId(), step1.getId(), "REJECT", "", testHrManager.getUsername())
        );
        assertTrue(ex.getMessage().contains("Bắt buộc phải nhập ý kiến"));

        // KN-236, KN-241: Từ chối có ý kiến thành công và ghi nhận lịch sử
        JobRequestApprovalStepDto rejectedStep = approvalWorkflowService.processApprovalStep(
                jobRequest.getId(), step1.getId(), "REJECT", "Chưa đủ ngân sách quý này", testHrManager.getUsername()
        );

        assertEquals("REJECTED", rejectedStep.getStatus());
        assertEquals("Chưa đủ ngân sách quý này", rejectedStep.getComment());
        assertNotNull(rejectedStep.getApprovalDate());
        assertEquals(testHrManager.getUsername(), rejectedStep.getActionByUserName());

        // Kiểm tra bước 2 bị CANCELLED
        List<JobRequestApprovalStepDto> updatedSteps = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        assertEquals("CANCELLED", updatedSteps.get(1).getStatus());

        // Kiểm tra JobRequest chuyển thành REJECTED
        JobRequest updatedJob = jobRequestRepository.findById(jobRequest.getId()).orElseThrow();
        assertEquals("REJECTED", updatedJob.getStatus());
    }

    @Test
    @DisplayName("KN-236, KN-237, KN-240, KN-241: Yêu cầu bổ sung bắt buộc nhập ý kiến, trả hồ sơ về người tạo và giữ nguyên lịch sử")
    void testRequestChange_KN236_KN237_KN240_KN241() {
        ApprovalRuleDto ruleDto = ApprovalRuleDto.builder()
                .name("Quy trình duyệt test yêu cầu bổ sung " + System.currentTimeMillis())
                .departmentId(testDept.getId())
                .active(true)
                .steps(List.of(
                        ApprovalRuleStepDto.builder().stepOrder(1).stepName("Bước 1: HR").approverRole("ROLE_HR_MANAGER").build(),
                        ApprovalRuleStepDto.builder().stepOrder(2).stepName("Bước 2: Giám đốc").approverRole("ROLE_ADMIN").build()
                ))
                .build();
        approvalWorkflowService.createRule(ruleDto, testAdmin.getUsername());

        JobRequestDto requestDto = JobRequestDto.builder()
                .departmentId(testDept.getId())
                .positionId(testPosition.getId())
                .title("Tuyển dụng kiểm thử yêu cầu bổ sung")
                .headcount(1)
                .recruitmentReason("REPLACEMENT")
                .neededDate(LocalDate.now().plusDays(30))
                .minSalary(22_000_000.0)
                .maxSalary(28_000_000.0)
                .description("Mô tả đầy đủ")
                .requirements("Yêu cầu đầy đủ")
                .build();

        JobRequestDto jobRequest = jobRequestService.createOrDraftJobRequest(requestDto, testHrManager.getUsername(), false);
        List<JobRequestApprovalStepDto> steps = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        JobRequestApprovalStepDto step1 = steps.get(0);

        // KN-237: Yêu cầu bổ sung nhưng không nhập ý kiến phải ném AppException
        AppException ex = assertThrows(AppException.class, () ->
                approvalWorkflowService.processApprovalStep(jobRequest.getId(), step1.getId(), "REQUEST_CHANGE", "   ", testHrManager.getUsername())
        );
        assertTrue(ex.getMessage().contains("Bắt buộc phải nhập ý kiến"));

        // KN-236, KN-240, KN-241: Yêu cầu bổ sung có ý kiến thành công
        JobRequestApprovalStepDto changedStep = approvalWorkflowService.processApprovalStep(
                jobRequest.getId(), step1.getId(), "REQUEST_CHANGE", "Cần làm rõ thêm yêu cầu số năm kinh nghiệm", testHrManager.getUsername()
        );

        assertEquals("REQUEST_CHANGE", changedStep.getStatus());
        assertEquals("Cần làm rõ thêm yêu cầu số năm kinh nghiệm", changedStep.getComment());
        assertNotNull(changedStep.getApprovalDate());
        assertEquals(testHrManager.getUsername(), changedStep.getActionByUserName());

        // KN-240: Hồ sơ được trả về người tạo ở trạng thái CHANGES_REQUESTED
        JobRequest updatedJob = jobRequestRepository.findById(jobRequest.getId()).orElseThrow();
        assertEquals("CHANGES_REQUESTED", updatedJob.getStatus());

        // KN-240: Lịch sử các bước được giữ nguyên, không bị huỷ hay xoá
        List<JobRequestApprovalStepDto> allSteps = approvalWorkflowService.getApprovalStepsForJobRequest(jobRequest.getId());
        assertEquals(2, allSteps.size());
        assertEquals("REQUEST_CHANGE", allSteps.get(0).getStatus());
        assertEquals("WAITING", allSteps.get(1).getStatus());
    }
}

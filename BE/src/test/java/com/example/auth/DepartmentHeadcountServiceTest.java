package com.example.auth;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.impl.DepartmentHeadcountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentHeadcountServiceTest {

    @Mock
    private DepartmentHeadcountPlanRepository planRepository;

    @Mock
    private HeadcountOverrideAuditRepository auditRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private JobRequestRepository jobRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentHeadcountServiceImpl headcountService;

    private Department department;
    private DepartmentHeadcountPlan plan;
    private User hrManager;
    private User hiringManager;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .code("TECH")
                .name("Phòng Công Nghệ")
                .build();

        plan = DepartmentHeadcountPlan.builder()
                .id(100L)
                .department(department)
                .year(2026)
                .targetHeadcount(5)
                .salaryBudget(BigDecimal.valueOf(200_000_000))
                .note("Kế hoạch công nghệ 2026")
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .build();

        hrManager = User.builder()
                .id(10L)
                .username("truongphongnhansu")
                .fullName("Trưởng Phòng Nhân Sự")
                .roles(Collections.singleton("ROLE_HR_MANAGER"))
                .build();

        hiringManager = User.builder()
                .id(11L)
                .username("truongbophan")
                .fullName("Trưởng Bộ Phận Kỹ Thuật")
                .roles(Collections.singleton("ROLE_HIRING_MANAGER"))
                .build();
    }

    @Test
    void testCreateOrUpdatePlan_success_KN223() {
        DepartmentHeadcountPlanDto dto = DepartmentHeadcountPlanDto.builder()
                .departmentId(1L)
                .year(2026)
                .targetHeadcount(10)
                .salaryBudget(BigDecimal.valueOf(500_000_000))
                .note("Khai báo chỉ tiêu năm 2026")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(planRepository.findByDepartmentIdAndYear(1L, 2026)).thenReturn(Optional.empty());
        when(planRepository.save(any(DepartmentHeadcountPlan.class))).thenAnswer(invocation -> {
            DepartmentHeadcountPlan saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(jobRequestRepository.findByDepartmentId(1L)).thenReturn(Collections.emptyList());

        DepartmentHeadcountPlanDto result = headcountService.createOrUpdatePlan(dto, "truongphongnhansu");

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(10, result.getTargetHeadcount());
        assertEquals(BigDecimal.valueOf(500_000_000), result.getSalaryBudget());
        assertEquals(10, result.getRemainingHeadcount()); // Không có request nào -> còn 10
        verify(planRepository, times(1)).save(any(DepartmentHeadcountPlan.class));
    }

    @Test
    void testCreateOrUpdatePlan_validationFailures_KN223() {
        // 1. Thiếu phòng ban
        DepartmentHeadcountPlanDto dto1 = DepartmentHeadcountPlanDto.builder()
                .year(2026)
                .targetHeadcount(5)
                .salaryBudget(BigDecimal.valueOf(100_000_000))
                .build();
        assertThrows(AppException.class, () -> headcountService.createOrUpdatePlan(dto1, "admin"));

        // 2. Headcount âm
        DepartmentHeadcountPlanDto dto2 = DepartmentHeadcountPlanDto.builder()
                .departmentId(1L)
                .year(2026)
                .targetHeadcount(-2)
                .salaryBudget(BigDecimal.valueOf(100_000_000))
                .build();
        assertThrows(AppException.class, () -> headcountService.createOrUpdatePlan(dto2, "admin"));

        // 3. Ngân sách âm
        DepartmentHeadcountPlanDto dto3 = DepartmentHeadcountPlanDto.builder()
                .departmentId(1L)
                .year(2026)
                .targetHeadcount(5)
                .salaryBudget(BigDecimal.valueOf(-500))
                .build();
        assertThrows(AppException.class, () -> headcountService.createOrUpdatePlan(dto3, "admin"));
    }

    @Test
    void testCheckDepartmentHeadcount_calculatesRemainingAndExceeded_KN224_KN225() {
        // Giả lập đã có 1 yêu cầu tuyển 3 người trong năm 2026
        JobRequest existingReq = JobRequest.builder()
                .id(50L)
                .department(department)
                .neededDate(LocalDate.of(2026, 6, 1))
                .headcount(3)
                .status("APPROVED")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(planRepository.findByDepartmentIdAndYear(1L, 2026)).thenReturn(Optional.of(plan)); // target = 5
        when(jobRequestRepository.findByDepartmentId(1L)).thenReturn(Collections.singletonList(existingReq));

        // Kiểm tra với yêu cầu tuyển thêm 2 người (tổng 3 + 2 = 5 <= 5 -> không vượt)
        HeadcountCheckResponseDto checkNotExceed = headcountService.checkDepartmentHeadcount(1L, 2026, 2, null);
        assertNotNull(checkNotExceed);
        assertEquals(5, checkNotExceed.getTargetHeadcount());
        assertEquals(3, checkNotExceed.getUsedHeadcount());
        assertEquals(2, checkNotExceed.getRemainingHeadcount());
        assertFalse(checkNotExceed.getWillExceed());
        assertEquals(0, checkNotExceed.getExceededCount());

        // Kiểm tra với yêu cầu tuyển thêm 4 người (tổng 3 + 4 = 7 > 5 -> vượt 2 người)
        HeadcountCheckResponseDto checkExceed = headcountService.checkDepartmentHeadcount(1L, 2026, 4, null);
        assertNotNull(checkExceed);
        assertTrue(checkExceed.getWillExceed());
        assertEquals(2, checkExceed.getExceededCount());
        assertNotNull(checkExceed.getWarningMessage());
        assertTrue(checkExceed.getWarningMessage().contains("vượt 2 chỉ tiêu"));
    }

    @Test
    void testValidateJobRequestHeadcount_blocksSubmitWhenExceededWithoutOverride_KN225() {
        // Chỉ tiêu 5, đã dùng 4, yêu cầu tuyển 2 -> vượt 1
        JobRequest existingReq = JobRequest.builder()
                .id(50L)
                .department(department)
                .neededDate(LocalDate.of(2026, 6, 1))
                .headcount(4)
                .status("PENDING_APPROVAL")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(planRepository.findByDepartmentIdAndYear(1L, 2026)).thenReturn(Optional.of(plan));
        when(jobRequestRepository.findByDepartmentId(1L)).thenReturn(Collections.singletonList(existingReq));

        JobRequest newReq = JobRequest.builder()
                .department(department)
                .neededDate(LocalDate.of(2026, 8, 1))
                .headcount(2)
                .status("PENDING_APPROVAL")
                .build();

        // Không có cờ ghi đè -> Phải ném ngoại lệ chặn lưu (KN-225)
        AppException ex = assertThrows(AppException.class, () ->
                headcountService.validateAndProcessJobRequestHeadcount(newReq, false, null, hiringManager)
        );
        assertTrue(ex.getMessage().contains("CẢNH BÁO VƯỢT CHỈ TIÊU (KN-225)"));
    }

    @Test
    void testValidateJobRequestHeadcount_allowsOverrideByHrManager_KN226_KN227() {
        // Chỉ tiêu 5, đã dùng 5, yêu cầu tuyển 2 -> vượt 2
        JobRequest existingReq = JobRequest.builder()
                .id(50L)
                .department(department)
                .neededDate(LocalDate.of(2026, 6, 1))
                .headcount(5)
                .status("APPROVED")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(planRepository.findByDepartmentIdAndYear(1L, 2026)).thenReturn(Optional.of(plan));
        when(jobRequestRepository.findByDepartmentId(1L)).thenReturn(Collections.singletonList(existingReq));

        JobRequest newReq = JobRequest.builder()
                .id(51L)
                .title("Tuyển Dev Senior dự án mới")
                .department(department)
                .neededDate(LocalDate.of(2026, 8, 1))
                .headcount(2)
                .status("PENDING_APPROVAL")
                .build();

        // HR Manager tích chọn ghi đè và nhập lý do -> Cho phép ghi đè trực tiếp (KN-226)
        headcountService.validateAndProcessJobRequestHeadcount(
                newReq, true, "Ban Giám Đốc phê duyệt bổ sung dự án ERP", hrManager);

        assertTrue(newReq.getIsOverHeadcount());
        assertTrue(newReq.getOverHeadcountApproved());
        assertEquals("truongphongnhansu", newReq.getOverHeadcountApprovedBy());
        assertNotNull(newReq.getOverHeadcountApprovedAt());
        assertEquals("Ban Giám Đốc phê duyệt bổ sung dự án ERP", newReq.getOverHeadcountReason());

        // Kiểm tra audit log đã được ghi nhận (KN-227)
        verify(auditRepository, times(1)).save(argThat(audit ->
                "DIRECT_OVERRIDE".equals(audit.getActionType()) &&
                audit.getExceededHeadcount() == 2 &&
                "truongphongnhansu".equals(audit.getActionBy())
        ));
    }

    @Test
    void testProcessHeadcountOverrideAction_approveByHrManager_KN226_KN227() {
        JobRequest pendingReq = JobRequest.builder()
                .id(60L)
                .title("Kỹ sư AI")
                .department(department)
                .headcount(3)
                .neededDate(LocalDate.of(2026, 9, 1))
                .isOverHeadcount(true)
                .overHeadcountApproved(false)
                .overHeadcountReason("Đề xuất mở rộng AI")
                .status("PENDING_APPROVAL")
                .build();

        when(userRepository.findByUsername("truongphongnhansu")).thenReturn(Optional.of(hrManager));
        when(jobRequestRepository.findById(60L)).thenReturn(Optional.of(pendingReq));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(planRepository.findByDepartmentIdAndYear(1L, 2026)).thenReturn(Optional.of(plan));
        when(jobRequestRepository.findByDepartmentId(1L)).thenReturn(Collections.emptyList());
        when(jobRequestRepository.save(any(JobRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HeadcountOverrideActionRequest req = HeadcountOverrideActionRequest.builder()
                .jobRequestId(60L)
                .approved(true)
                .reason("Đồng ý bổ sung chỉ tiêu đặc cách")
                .build();

        JobRequestDto result = headcountService.processHeadcountOverrideAction(req, "truongphongnhansu");

        assertNotNull(result);
        assertTrue(result.getIsOverHeadcount());
        assertTrue(result.getOverHeadcountApproved());
        assertEquals("truongphongnhansu", result.getOverHeadcountApprovedBy());

        // Kiểm tra audit log APPROVE_OVERRIDE
        verify(auditRepository, times(1)).save(argThat(audit ->
                "APPROVE_OVERRIDE".equals(audit.getActionType()) &&
                audit.getReason().contains("Đồng ý bổ sung")
        ));
    }

    @Test
    void testProcessHeadcountOverrideAction_rejectByHrManager_KN226() {
        JobRequest pendingReq = JobRequest.builder()
                .id(60L)
                .title("Kỹ sư AI")
                .department(department)
                .headcount(3)
                .neededDate(LocalDate.of(2026, 9, 1))
                .isOverHeadcount(true)
                .overHeadcountApproved(false)
                .status("PENDING_APPROVAL")
                .build();

        when(userRepository.findByUsername("truongphongnhansu")).thenReturn(Optional.of(hrManager));
        when(jobRequestRepository.findById(60L)).thenReturn(Optional.of(pendingReq));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(planRepository.findByDepartmentIdAndYear(1L, 2026)).thenReturn(Optional.of(plan));
        when(jobRequestRepository.findByDepartmentId(1L)).thenReturn(Collections.emptyList());
        when(jobRequestRepository.save(any(JobRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HeadcountOverrideActionRequest req = HeadcountOverrideActionRequest.builder()
                .jobRequestId(60L)
                .approved(false)
                .reason("Không đủ ngân sách lương quý này")
                .build();

        JobRequestDto result = headcountService.processHeadcountOverrideAction(req, "truongphongnhansu");

        assertNotNull(result);
        assertFalse(result.getOverHeadcountApproved());
        assertEquals("REJECTED", result.getStatus());

        // Kiểm tra audit log REJECT_OVERRIDE
        verify(auditRepository, times(1)).save(argThat(audit ->
                "REJECT_OVERRIDE".equals(audit.getActionType()) &&
                audit.getReason().contains("Không đủ ngân sách")
        ));
    }
}

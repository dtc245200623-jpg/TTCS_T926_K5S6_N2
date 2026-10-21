package com.example.auth.service;

import com.example.auth.dto.*;
import com.example.auth.entity.JobRequest;
import com.example.auth.entity.User;

import java.util.List;

public interface DepartmentHeadcountService {

    /**
     * KN-223: Khai báo / cập nhật chỉ tiêu headcount và ngân sách lương theo phòng ban theo năm
     */
    DepartmentHeadcountPlanDto createOrUpdatePlan(DepartmentHeadcountPlanDto dto, String currentUsername);

    DepartmentHeadcountPlanDto getPlanByDepartmentAndYear(Long departmentId, Integer year);

    List<DepartmentHeadcountPlanDto> getPlansByYear(Integer year);

    List<DepartmentHeadcountPlanDto> getAllPlans();

    void deletePlan(Long id);

    /**
     * KN-224, KN-225: Kiểm tra số headcount còn lại và cảnh báo nếu vượt chỉ tiêu
     */
    HeadcountCheckResponseDto checkDepartmentHeadcount(Long departmentId, Integer year, Integer requestedHeadcount, Long excludeJobRequestId);

    /**
     * KN-225, KN-226: Kiểm tra tính hợp lệ và xử lý ghi đè khi submit yêu cầu tuyển dụng
     */
    void validateAndProcessJobRequestHeadcount(JobRequest jobRequest, Boolean isOverrideConfirmed, String overrideReason, User currentUser);

    /**
     * KN-226: Trưởng phòng Nhân sự duyệt hoặc từ chối ghi đè chỉ tiêu headcount
     */
    JobRequestDto processHeadcountOverrideAction(HeadcountOverrideActionRequest request, String currentUsername);

    /**
     * KN-227: Ghi nhận audit log ban đầu sau khi JobRequest đã được persist và có ID
     */
    void recordInitialOverrideAudit(JobRequest jobRequest, String reason, User currentUser);

    /**
     * KN-227: Lấy lịch sử phê duyệt và ghi đè chỉ tiêu phục vụ truy vết / kiểm toán
     */
    List<HeadcountOverrideAuditDto> getOverrideAudits(Long jobRequestId, Long departmentId, Integer year);
}

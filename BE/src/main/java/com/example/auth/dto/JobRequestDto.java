package com.example.auth.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestDto {
    private Long id;
    private String title;
    private Integer headcount;
    private Double minSalary;
    private Double maxSalary;
    private String description;
    private String requirements;
    private String status;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String positionCode;
    private Double positionMinSalary;
    private Double positionMaxSalary;
    private String recruitmentReason; // REPLACEMENT, NEW_HEADCOUNT
    private LocalDate neededDate;
    private String salaryExplanation;
    private Boolean isDraft;
    private Integer currentApprovalStep;
    private Integer totalApprovalSteps;
    private Long approvalRuleId;
    private java.util.List<JobRequestApprovalStepDto> approvalSteps;
    private Long hiringManagerId;
    private String hiringManagerName;
    private Long recruiterId;
    private String recruiterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // KN-225, KN-226: Ghi đè chỉ tiêu headcount
    private Boolean isOverHeadcount;
    private Boolean overHeadcountApproved;
    private String overHeadcountReason;
    private String overHeadcountApprovedBy;
    private LocalDateTime overHeadcountApprovedAt;
    private Boolean isOverHeadcountConfirmed;

    // KN-244: Xác định và hiển thị người đang chờ phê duyệt ở cấp tiếp theo
    private String currentApproverName;
    private String currentApproverRole;
    private String currentStepName;

    // KN-246, KN-247, KN-248, KN-249: Đóng yêu cầu và cảnh báo ứng viên
    private String closingReason;
    private String closingNote;
    private LocalDateTime closedAt;
    private String closedBy;
    private Integer activeApplicationsCount;

    // Đăng tin & Cổng ứng tuyển - Nhân viên tuyển dụng
    private Long locationId;
    private String locationName;
    private Long employmentTypeId;
    private String employmentTypeName;
    private LocalDate applicationDeadline;
    private Boolean isSalaryVisible;
    private LocalDateTime publishedAt;
    private String publishedBy;
}

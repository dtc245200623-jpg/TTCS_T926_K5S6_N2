package com.example.auth.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestApprovalStepDto {
    private Long id;
    private Long jobRequestId;
    private Integer stepOrder;
    private String stepName;
    private String approverRole;
    private Long approverUserId;
    private String approverUserName;
    private String approverUserFullName;
    private String status; // PENDING, APPROVED, REJECTED, WAITING, CANCELLED
    private Boolean isOverBudgetStep;
    private LocalDateTime approvalDate;
    private String comment;
    private Long actionByUserId;
    private String actionByUserName;
    private String actionByUserFullName;
    private LocalDateTime createdAt;
}

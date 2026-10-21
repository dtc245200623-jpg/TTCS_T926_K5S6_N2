package com.example.auth.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRuleDto {
    private Long id;
    private String name;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private Double minSalary;
    private Double maxSalary;
    private Double budgetLimit;
    private Boolean autoEscalateOverBudget;
    private String overBudgetStepName;
    private String overBudgetRole;
    private Boolean active;
    private Integer priority;
    private String description;
    private List<ApprovalRuleStepDto> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

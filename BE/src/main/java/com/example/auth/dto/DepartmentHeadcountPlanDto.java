package com.example.auth.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentHeadcountPlanDto {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private Integer year;
    private Integer targetHeadcount;
    private BigDecimal salaryBudget;
    private String note;

    // Các trường tính toán thời gian thực
    private Integer usedHeadcount;
    private Integer remainingHeadcount;
    private BigDecimal currentSalaryUsed;
    private BigDecimal remainingSalaryBudget;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.auth.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * KN-224, KN-225: DTO phản hồi kết quả kiểm tra chỉ tiêu và số headcount còn lại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadcountCheckResponseDto {
    private Long departmentId;
    private String departmentName;
    private Integer year;
    private Boolean hasPlan;
    private Integer targetHeadcount;
    private Integer usedHeadcount;
    private Integer remainingHeadcount;
    private BigDecimal salaryBudget;

    // Khi người dùng nhập số lượng cần tuyển:
    private Integer requestedHeadcount;
    private Boolean willExceed;
    private Integer exceededCount;
    private String warningMessage;
}

package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * KN-227: DTO lịch sử ghi đè và kiểm toán chỉ tiêu headcount
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadcountOverrideAuditDto {
    private Long id;
    private Long jobRequestId;
    private String jobRequestTitle;
    private Long departmentId;
    private String departmentName;
    private Integer year;
    private Integer targetHeadcount;
    private Integer usedHeadcountBefore;
    private Integer requestedHeadcount;
    private Integer exceededHeadcount;
    private String actionType;
    private String actionTypeName;
    private String reason;
    private String actionBy;
    private String actionByRole;
    private LocalDateTime createdAt;
}

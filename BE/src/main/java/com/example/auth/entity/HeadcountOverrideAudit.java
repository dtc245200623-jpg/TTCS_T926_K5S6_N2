package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * KN-227: Ghi nhận lịch sử phê duyệt và ghi đè chỉ tiêu để phục vụ truy vết / kiểm toán
 */
@Entity
@Table(name = "headcount_override_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadcountOverrideAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_request_id", nullable = false)
    private Long jobRequestId;

    @Column(name = "job_request_title", columnDefinition = "NVARCHAR(255)")
    private String jobRequestTitle;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "department_name", columnDefinition = "NVARCHAR(255)")
    private String departmentName;

    @Column(name = "plan_year", nullable = false)
    private Integer year;

    @Column(name = "target_headcount")
    private Integer targetHeadcount;

    @Column(name = "used_headcount_before")
    private Integer usedHeadcountBefore;

    @Column(name = "requested_headcount")
    private Integer requestedHeadcount;

    @Column(name = "exceeded_headcount")
    private Integer exceededHeadcount;

    /**
     * REQUEST_OVERRIDE (Đề xuất vượt chỉ tiêu),
     * APPROVE_OVERRIDE (Xác nhận ghi đè),
     * REJECT_OVERRIDE (Từ chối ghi đè),
     * DIRECT_OVERRIDE (HR Manager tạo ghi đè trực tiếp)
     */
    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "reason", columnDefinition = "NVARCHAR(MAX)")
    private String reason;

    @Column(name = "action_by", length = 100, nullable = false)
    private String actionBy;

    @Column(name = "action_by_role", length = 100)
    private String actionByRole;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

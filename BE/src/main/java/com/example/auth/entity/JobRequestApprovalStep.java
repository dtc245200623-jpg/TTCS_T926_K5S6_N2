package com.example.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_request_approval_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequestApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Yêu cầu tuyển dụng gắn liền với bước duyệt này (KN-235)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_request_id", nullable = false)
    @JsonIgnore
    private JobRequest jobRequest;

    // Thứ tự bước duyệt
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    // Tên bước duyệt snapshot
    @Column(name = "step_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String stepName;

    // Vai trò duyệt snapshot
    @Column(name = "approver_role", length = 50)
    private String approverRole;

    // Người duyệt chỉ định snapshot
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approverUser;

    // Trạng thái: PENDING (Đang chờ duyệt bước này), APPROVED (Đã duyệt), REJECTED (Bị từ chối), WAITING (Chờ bước trước), CANCELLED
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "WAITING";

    // Cấp duyệt cao hơn được bổ sung tự động khi vượt hạn mức (KN-221)
    @Column(name = "is_over_budget_step")
    @Builder.Default
    private Boolean isOverBudgetStep = false;

    // Thời điểm duyệt
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    // Ý kiến phê duyệt / từ chối
    @Column(name = "comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    // Người trực tiếp thực hiện hành động duyệt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by_user_id")
    private User actionBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "WAITING";
        if (isOverBudgetStep == null) isOverBudgetStep = false;
    }
}

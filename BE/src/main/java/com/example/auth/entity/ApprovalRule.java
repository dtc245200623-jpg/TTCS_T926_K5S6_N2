package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String name;

    // Áp dụng cho phòng ban cụ thể (nếu null -> áp dụng cho tất cả phòng ban)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Ngưỡng mức lương đề xuất áp dụng (KN-220)
    @Column(name = "min_salary")
    private Double minSalary;

    @Column(name = "max_salary")
    private Double maxSalary;

    // Hạn mức ngân sách / trần lương tiêu chuẩn của quy tắc (KN-221)
    @Column(name = "budget_limit")
    private Double budgetLimit;

    // Tự động thêm cấp duyệt cao hơn khi vượt hạn mức (KN-221)
    @Column(name = "auto_escalate_over_budget")
    @Builder.Default
    private Boolean autoEscalateOverBudget = true;

    @Column(name = "over_budget_step_name", columnDefinition = "NVARCHAR(255)")
    @Builder.Default
    private String overBudgetStepName = "Ban Giám đốc phê duyệt vượt hạn mức";

    @Column(name = "over_budget_role", length = 50)
    @Builder.Default
    private String overBudgetRole = "ROLE_ADMIN";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<ApprovalRuleStep> steps = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (autoEscalateOverBudget == null) autoEscalateOverBudget = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

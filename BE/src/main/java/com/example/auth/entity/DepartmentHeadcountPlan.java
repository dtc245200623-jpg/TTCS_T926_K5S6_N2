package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * KN-223: Khai báo chỉ tiêu headcount và ngân sách lương theo phòng ban theo năm
 */
@Entity
@Table(name = "department_headcount_plans", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"department_id", "plan_year"}, name = "uk_dept_plan_year")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentHeadcountPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "plan_year", nullable = false)
    private Integer year;

    @Column(name = "target_headcount", nullable = false)
    private Integer targetHeadcount;

    @Column(name = "salary_budget", precision = 18, scale = 2)
    private BigDecimal salaryBudget;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (targetHeadcount == null) targetHeadcount = 0;
        if (salaryBudget == null) salaryBudget = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

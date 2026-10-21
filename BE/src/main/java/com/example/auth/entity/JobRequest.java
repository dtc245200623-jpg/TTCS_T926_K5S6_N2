package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "NVARCHAR(255)")
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private JobPosition position;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hiring_manager_id")
    private User hiringManager;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id")
    private User recruiter;

    // KN-190: Địa điểm làm việc tham chiếu tới danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private RecruitmentCategory location;

    // KN-191: Hình thức làm việc tham chiếu tới danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employment_type_id")
    private RecruitmentCategory employmentType;
    
    private Integer headcount;
    private Double minSalary;
    private Double maxSalary;
    
    // KN-206: Lý do tuyển dụng (REPLACEMENT - Thay thế hoặc NEW_HEADCOUNT - Tăng mới)
    @Column(name = "recruitment_reason", length = 50)
    private String recruitmentReason;

    // KN-206, KN-208: Ngày cần người
    @Column(name = "needed_date")
    private LocalDate neededDate;

    // KN-208: Giải trình dải lương khi nằm ngoài dải chuẩn của chức danh
    @Column(name = "salary_explanation", columnDefinition = "NVARCHAR(MAX)")
    private String salaryExplanation;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String requirements;
    
    @Column(length = 50)
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, OPEN, CLOSED
    
    // KN-220, KN-221, KN-235: Theo dõi luồng duyệt nhiều cấp
    @Column(name = "current_approval_step")
    private Integer currentApprovalStep;

    @Column(name = "total_approval_steps")
    private Integer totalApprovalSteps;

    @Column(name = "approval_rule_id")
    private Long approvalRuleId;

    // KN-225, KN-226: Cờ và thông tin ghi đè vượt chỉ tiêu headcount
    @Column(name = "is_over_headcount")
    @Builder.Default
    private Boolean isOverHeadcount = false;

    @Column(name = "over_headcount_approved")
    @Builder.Default
    private Boolean overHeadcountApproved = false;

    @Column(name = "over_headcount_reason", columnDefinition = "NVARCHAR(MAX)")
    private String overHeadcountReason;

    @Column(name = "over_headcount_approved_by", length = 100)
    private String overHeadcountApprovedBy;

    @Column(name = "over_headcount_approved_at")
    private LocalDateTime overHeadcountApprovedAt;

    // KN-246: 3 trạng thái kết thúc (FILLED - Đã tuyển đủ, PAUSED - Tạm dừng, CANCELLED - Huỷ)
    @Column(name = "closing_reason", length = 50)
    private String closingReason;

    @Column(name = "closing_note", columnDefinition = "NVARCHAR(MAX)")
    private String closingNote;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    // Đăng tin & Cổng ứng tuyển - Nhân viên tuyển dụng
    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "is_salary_visible")
    @Builder.Default
    private Boolean isSalaryVisible = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @OneToMany(mappedBy = "jobRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private java.util.List<JobRequestApprovalStep> approvalSteps = new java.util.ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

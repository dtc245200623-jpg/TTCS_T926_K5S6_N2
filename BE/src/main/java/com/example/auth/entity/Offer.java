package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
    
    private Double offeredSalary;
    private LocalDateTime expectedStartDate;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;
    
    @Column(length = 50)
    private String status; // PENDING_APPROVAL, APPROVED, SENT, ACCEPTED, DECLINED

    // KN-179: Giới hạn duyệt offer theo dải lương của chức danh
    @Column(name = "exceeds_limit")
    @Builder.Default
    private Boolean exceedsLimit = false;

    @Column(name = "max_salary_allowed")
    private Double maxSalaryAllowed;

    @Column(name = "approval_note", columnDefinition = "NVARCHAR(MAX)")
    private String approvalNote;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (exceedsLimit == null) exceedsLimit = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

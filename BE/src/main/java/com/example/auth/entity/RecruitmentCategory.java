package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruitment_categories", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_category_type_code", columnNames = {"type", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Loại danh mục: CANDIDATE_SOURCE, REJECTION_REASON, WORK_LOCATION, EMPLOYMENT_TYPE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoryType type;

    // Mã danh mục (duy nhất trong cùng loại)
    @Column(nullable = false, length = 50)
    private String code;

    // Tên hiển thị
    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String name;

    // Mô tả chi tiết
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // KN-193: Thứ tự hiển thị
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    // Trạng thái kích hoạt (Đang áp dụng / Tạm dừng)
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Danh mục mặc định của hệ thống
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (isSystem == null) {
            isSystem = false;
        }
        if (displayOrder == null) {
            displayOrder = 1;
        }
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }
}

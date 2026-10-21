package com.example.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_rule_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRuleStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @JsonIgnore
    private ApprovalRule rule;

    // Thứ tự bước duyệt (1, 2, 3...)
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    // Tên bước duyệt (VD: "Trưởng phòng Nhân sự thẩm định", "Giám đốc Khối phê duyệt")
    @Column(name = "step_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String stepName;

    // Vai trò duyệt (VD: "ROLE_HR_MANAGER", "ROLE_APPROVER", "ROLE_ADMIN")
    @Column(name = "approver_role", length = 50)
    private String approverRole;

    // Người dùng duyệt chỉ định cụ thể (tùy chọn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approverUser;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String description;
}

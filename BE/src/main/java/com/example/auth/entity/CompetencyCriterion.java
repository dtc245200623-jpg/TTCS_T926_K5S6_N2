package com.example.auth.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "competency_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "framework_id", nullable = false)
    @JsonBackReference
    private CompetencyFramework framework;

    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // KN-183: Trọng số của tiêu chí (tổng trọng số của một khung bằng 100%)
    @Column(nullable = false)
    private Double weight;

    // Thang điểm tối đa (ví dụ 10)
    @Column(name = "scale_max", nullable = false)
    @Builder.Default
    private Integer scaleMax = 10;

    // Thứ tự hiển thị
    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 1;

    // KN-186: Danh sách câu hỏi phỏng vấn gắn với tiêu chí này
    @OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<InterviewQuestion> questions = new java.util.ArrayList<>();
}

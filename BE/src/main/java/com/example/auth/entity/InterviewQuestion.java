package com.example.auth.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KN-186: Mỗi câu hỏi gắn với một tiêu chí trong khung năng lực
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    @JsonBackReference
    private CompetencyCriterion criterion;

    // Nội dung câu hỏi phỏng vấn
    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String content;

    // Mức độ khó: EASY (Dễ), MEDIUM (Trung bình), HARD (Khó)
    @Column(nullable = false, length = 50)
    private String difficulty;

    // Gợi ý câu trả lời tốt / Tiêu chuẩn đánh giá câu trả lời đạt
    @Column(name = "good_answer_guideline", columnDefinition = "NVARCHAR(MAX)")
    private String goodAnswerGuideline;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

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
        if (difficulty != null) {
            difficulty = difficulty.trim().toUpperCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (difficulty != null) {
            difficulty = difficulty.trim().toUpperCase();
        }
    }
}

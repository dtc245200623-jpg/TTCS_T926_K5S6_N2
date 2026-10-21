package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne @JoinColumn(name = "interview_id")
    private Interview interview;

    // KN-184: Khung năng lực dùng để đánh giá phỏng vấn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "framework_id")
    private CompetencyFramework framework;
    
    private Integer score; // Điểm trung bình thang 1-10
    
    // KN-183 & KN-184: Điểm tổng hợp có tính trọng số (%)
    @Column(name = "weighted_score")
    private Double weightedScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    @Column(length = 50)
    private String recommendedAction; // HIRE, REJECT, NEXT_ROUND
    
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<EvaluationCriterionScore> criterionScores = new java.util.ArrayList<>();

    @Column(name = "evaluated_at")
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}

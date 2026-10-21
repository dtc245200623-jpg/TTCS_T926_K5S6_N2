package com.example.auth.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_criterion_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    @JsonBackReference
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id")
    private CompetencyCriterion criterion;

    @Column(name = "criterion_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String criterionName;

    @Column(nullable = false)
    private Double weight;

    // Điểm số người phỏng vấn chấm (ví dụ: 1.0 - 10.0)
    @Column(nullable = false)
    private Double score;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;
}

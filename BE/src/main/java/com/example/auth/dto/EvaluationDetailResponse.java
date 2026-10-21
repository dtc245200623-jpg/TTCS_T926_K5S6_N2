package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluationDetailResponse {

    private Long id;
    private Long interviewId;
    private String candidateName;
    private String interviewerName;
    private String positionName;
    private String frameworkName;

    private Integer score; // Điểm trung bình làm tròn thang 1-10
    private Double weightedScore; // Điểm có tính trọng số chính xác (ví dụ 8.45/10)
    private String feedback;
    private String recommendedAction;
    private LocalDateTime evaluatedAt;

    @Builder.Default
    private List<CriterionScoreDetailDto> criterionScores = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionScoreDetailDto {
        private Long id;
        private Long criterionId;
        private String criterionName;
        private Double weight;
        private Double score;
        private String comment;
    }
}

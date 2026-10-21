package com.example.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationSubmitRequest {

    @NotNull(message = "ID buổi phỏng vấn không được để trống")
    private Long interviewId;

    private Long frameworkId;

    private String feedback;

    @NotBlank(message = "Kết luận đề xuất không được để trống")
    private String recommendedAction; // HIRE, REJECT, NEXT_ROUND, CONSIDER

    @Valid
    @Builder.Default
    private List<CriterionScoreInputDto> criteriaScores = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionScoreInputDto {
        private Long criterionId;

        @NotBlank(message = "Tên tiêu chí không được để trống")
        private String criterionName;

        @NotNull(message = "Trọng số không được để trống")
        private Double weight;

        @NotNull(message = "Điểm số không được để trống")
        @DecimalMin(value = "0.0", message = "Điểm số không được nhỏ hơn 0")
        @DecimalMax(value = "10.0", message = "Điểm số không được lớn hơn 10")
        private Double score;

        private String comment;
    }
}

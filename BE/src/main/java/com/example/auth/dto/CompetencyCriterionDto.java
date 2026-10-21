package com.example.auth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyCriterionDto {

    private Long id;

    @NotBlank(message = "Tên tiêu chí không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Trọng số tiêu chí không được để trống")
    @DecimalMin(value = "0.1", message = "Trọng số tiêu chí phải lớn hơn 0%")
    @DecimalMax(value = "100.0", message = "Trọng số tiêu chí không được vượt quá 100%")
    private Double weight;

    @Builder.Default
    private Integer scaleMax = 10;

    @Builder.Default
    private Integer orderIndex = 1;

    // KN-186: Số lượng câu hỏi và danh sách câu hỏi gợi ý gắn với tiêu chí
    private Integer questionCount;

    private List<InterviewQuestionResponse> questions;
}


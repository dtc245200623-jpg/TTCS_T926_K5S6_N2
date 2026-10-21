package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestionRequest {

    // KN-186: Tiêu chí năng lực gắn với câu hỏi
    @NotNull(message = "Tiêu chí năng lực không được để trống")
    private Long criterionId;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    // KN-186: Mức độ khó: EASY, MEDIUM, HARD
    @NotBlank(message = "Mức độ khó không được để trống")
    @Pattern(regexp = "^(?i)(EASY|MEDIUM|HARD)$", message = "Mức độ khó phải là EASY, MEDIUM hoặc HARD")
    private String difficulty;

    // Gợi ý câu trả lời tốt / Tiêu chuẩn đánh giá
    private String goodAnswerGuideline;

    @Builder.Default
    private Boolean active = true;
}

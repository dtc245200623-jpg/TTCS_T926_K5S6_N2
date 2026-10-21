package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewQuestionResponse {

    private Long id;

    // Tiêu chí năng lực liên kết (KN-186)
    private Long criterionId;
    private String criterionName;
    private Double criterionWeight;

    // Khung năng lực cha (KN-185)
    private Long frameworkId;
    private String frameworkCode;
    private String frameworkName;

    // Nội dung câu hỏi
    private String content;

    // Mức độ khó: EASY, MEDIUM, HARD
    private String difficulty;
    private String difficultyLabel;

    // Gợi ý câu trả lời tốt / Hướng dẫn chấm điểm
    private String goodAnswerGuideline;

    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

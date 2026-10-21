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
public class EvaluationTemplateResponse {

    // Thông tin phỏng vấn (nếu sinh từ phỏng vấn)
    private Long interviewId;
    private String candidateName;
    private String candidateEmail;
    private String interviewerName;
    private LocalDateTime scheduledAt;

    // Thông tin yêu cầu tuyển dụng & chức danh
    private Long jobRequestId;
    private String jobRequestTitle;
    private Long positionId;
    private String positionName;
    private String positionLevel;
    private String departmentName;

    // Thông tin khung năng lực sinh ra phiếu
    private Long frameworkId;
    private String frameworkCode;
    private String frameworkName;
    private String frameworkDescription;

    // Danh sách tiêu chí đánh giá kèm trọng số
    @Builder.Default
    private List<CompetencyCriterionDto> criteria = new ArrayList<>();

    private Double totalWeight;
    private String instructions;
}

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
public class CompetencyFrameworkResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;

    // KN-183: Tổng trọng số các tiêu chí (Phải bằng 100%)
    private Double totalWeight;
    private Integer criteriaCount;

    @Builder.Default
    private List<CompetencyCriterionDto> criteria = new ArrayList<>();

    // KN-182 & KN-184: Danh sách các chức danh dùng chung khung năng lực này
    @Builder.Default
    private List<PositionSummaryDto> positions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

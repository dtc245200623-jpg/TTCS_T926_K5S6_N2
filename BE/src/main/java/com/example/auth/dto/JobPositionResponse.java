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
public class JobPositionResponse {

    private Long id;
    private String code;
    private String name;
    private String level;

    private Long departmentId;
    private String departmentName;

    // KN-182 & KN-184: Khung năng lực áp dụng cho chức danh
    private Long competencyFrameworkId;
    private String competencyFrameworkName;
    private String competencyFrameworkCode;

    // KN-178 & KN-180: Dải lương được kiểm soát quyền hiển thị
    private Double minSalary;
    private Double maxSalary;
    private Boolean isSalaryMasked;

    private String description;
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

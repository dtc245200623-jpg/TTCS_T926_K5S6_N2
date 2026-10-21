package com.example.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyFrameworkRequest {

    @NotBlank(message = "Mã khung năng lực không được để trống")
    @Size(max = 50, message = "Mã khung năng lực không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên khung năng lực không được để trống")
    @Size(max = 255, message = "Tên khung năng lực không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @Builder.Default
    private Boolean active = true;

    // KN-183: Danh sách tiêu chí đánh giá và trọng số (tổng trọng số phải bằng 100%)
    @Valid
    @Builder.Default
    private List<CompetencyCriterionDto> criteria = new ArrayList<>();

    // KN-184: Danh sách chức danh được ánh xạ áp dụng khung năng lực này
    @Builder.Default
    private List<Long> positionIds = new ArrayList<>();
}

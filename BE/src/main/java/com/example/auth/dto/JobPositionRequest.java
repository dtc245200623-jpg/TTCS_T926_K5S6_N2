package com.example.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPositionRequest {

    @NotBlank(message = "Mã chức danh không được để trống")
    @Size(max = 50, message = "Mã chức danh không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên chức danh không được để trống")
    @Size(max = 255, message = "Tên chức danh không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Cấp bậc chức danh không được để trống")
    @Size(max = 50, message = "Cấp bậc không được vượt quá 50 ký tự")
    private String level;

    private Long departmentId;

    // KN-182 & KN-184: Khung năng lực áp dụng cho chức danh
    private Long competencyFrameworkId;

    // KN-178: Dải lương tối thiểu / tối đa
    @DecimalMin(value = "0.0", message = "Mức lương tối thiểu không được âm")
    private Double minSalary;

    @DecimalMin(value = "0.0", message = "Mức lương tối đa không được âm")
    private Double maxSalary;

    private String description;

    @Builder.Default
    private Boolean active = true;
}

package com.example.auth.dto;

import com.example.auth.entity.CategoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentCategoryRequest {

    @NotNull(message = "Loại danh mục không được để trống")
    private CategoryType type;

    @NotBlank(message = "Mã danh mục không được để trống")
    private String code;

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private String description;

    // KN-193: Thứ tự hiển thị
    @Min(value = 1, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 1")
    @Builder.Default
    private Integer displayOrder = 1;

    @Builder.Default
    private Boolean active = true;
}

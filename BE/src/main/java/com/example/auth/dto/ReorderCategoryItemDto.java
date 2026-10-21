package com.example.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderCategoryItemDto {

    @NotNull(message = "ID danh mục không được để trống")
    private Long id;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    @Min(value = 1, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 1")
    private Integer displayOrder;
}

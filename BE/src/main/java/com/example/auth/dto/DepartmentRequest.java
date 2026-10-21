package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequest {

    @Size(max = 50, message = "Mã phòng ban không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên phòng ban không được để trống")
    @Size(max = 255, message = "Tên phòng ban không được vượt quá 255 ký tự")
    private String name;

    private String description;

    private Long parentId;

    private Long managerId;

    @Builder.Default
    private Boolean active = true;
}

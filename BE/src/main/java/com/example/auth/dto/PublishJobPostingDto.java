package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishJobPostingDto {

    @NotBlank(message = "Tiêu đề tin tuyển dụng không được để trống")
    private String title;

    @NotBlank(message = "Nội dung mô tả công việc không được để trống")
    private String description;

    @NotBlank(message = "Yêu cầu tuyển dụng không được để trống")
    private String requirements;

    @NotNull(message = "Vui lòng chọn địa điểm làm việc")
    private Long locationId;

    @NotNull(message = "Vui lòng chọn hình thức làm việc")
    private Long employmentTypeId;

    @NotNull(message = "Vui lòng chọn hạn nhận hồ sơ")
    private LocalDate applicationDeadline;

    @Builder.Default
    private Boolean isSalaryVisible = true;
}

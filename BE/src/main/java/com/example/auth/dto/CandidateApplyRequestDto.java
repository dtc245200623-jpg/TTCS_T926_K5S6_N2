package com.example.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Biểu mẫu ứng tuyển trực tuyến dành cho ứng viên (KN-279, KN-280)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateApplyRequestDto {

    @NotNull(message = "Mã vị trí tuyển dụng không được để trống")
    private Long jobRequestId;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (VD: 0912345678 hoặc +84912345678)")
    private String phone;

    private Long sourceId;

    @Size(max = 4000, message = "Thư giới thiệu tối đa 4000 ký tự")
    private String coverLetter;
}

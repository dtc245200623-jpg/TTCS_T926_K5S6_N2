package com.example.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Biểu mẫu tra cứu trạng thái ứng tuyển bằng mã tra cứu và email (KN-291)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTrackingRequestDto {

    @NotBlank(message = "Mã tra cứu không được để trống")
    private String trackingCode;

    @NotBlank(message = "Email ứng tuyển không được để trống")
    @Email(message = "Email không đúng định dạng hợp lệ")
    private String email;
}

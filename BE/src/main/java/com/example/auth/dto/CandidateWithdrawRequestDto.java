package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * KN-293, KN-294: DTO yêu cầu rút hồ sơ của ứng viên
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateWithdrawRequestDto {

    @NotBlank(message = "Mã tra cứu không được để trống")
    private String trackingCode;

    @NotBlank(message = "Email không được để trống")
    private String email;

    private String reason;

    private Boolean confirm;
}

package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dành cho Recruiter xác nhận hoặc chỉnh sửa dữ liệu bóc tách từ CV (KN-287, KN-288)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmParsedDataDto {
    private String fullName;
    private String email;
    private String phone;
    
    // true = đồng ý ghi đè vào hồ sơ ứng viên; false = chỉ xác nhận đã xem
    private Boolean applyToProfile;
}

package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kết quả nộp hồ sơ thành công trả về cho ứng viên (KN-284)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateApplyResponseDto {
    private String trackingCode;
    private Long applicationId;
    private Long jobRequestId;
    private String jobTitle;
    private String candidateName;
    private String candidateEmail;
    private LocalDateTime appliedAt;
    private String cvUrl;
    private String message;
}

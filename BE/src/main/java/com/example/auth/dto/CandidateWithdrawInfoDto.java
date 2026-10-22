package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * KN-293, KN-294: DTO thông tin ứng tuyển trước khi xác nhận rút hồ sơ
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateWithdrawInfoDto {
    private Long applicationId;
    private String trackingCode;
    private String candidateName;
    private String candidateEmail;
    private Long jobId;
    private String jobTitle;
    private String departmentName;
    private LocalDateTime appliedAt;
    private String status;
    private Boolean isAlreadyWithdrawn;
    private String withdrawalReason;
    private LocalDateTime withdrawnAt;
}

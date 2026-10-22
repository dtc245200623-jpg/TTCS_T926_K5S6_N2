package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * KN-306: DTO lịch sử các lần ứng tuyển trước đây của cùng người này
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateHistoryDto {
    private Long applicationId;
    private Long jobRequestId;
    private String jobTitle;
    private String departmentName;
    private LocalDateTime appliedAt;
    private String status;
    private String generalStatus;
    private String cvUrl;
    private String rejectionNote;
    private String withdrawalReason;
}

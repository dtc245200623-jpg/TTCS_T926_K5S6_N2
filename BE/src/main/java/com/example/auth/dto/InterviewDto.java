package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InterviewDto {
    private Long id;
    private Long applicationId;
    private String candidateName;
    private String jobTitle;
    private String interviewerName;
    private LocalDateTime scheduledAt;
    private String status; // SCHEDULED, COMPLETED, CANCELLED
    private String feedback; // Interviewer feedback
    private String location;
    private String meetingLink;
}

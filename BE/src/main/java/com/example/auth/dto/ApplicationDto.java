package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApplicationDto {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private Long jobRequestId;
    private String jobTitle;
    private String status;
    private LocalDateTime appliedAt;
    private String interviewScore;
    private String cvUrl;
    private String cvOriginalFilename;
    private String trackingCode;
    private String coverLetter;
    private Long sourceId;
    private String sourceName;
    
    // CV Parsing fields for Recruiter
    private String parsedFullName;
    private String parsedEmail;
    private String parsedPhone;
    private String cvParsingStatus;
    private String cvParsingNote;
    private Boolean parsedDataConfirmed;
}

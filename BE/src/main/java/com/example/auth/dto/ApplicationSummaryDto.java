package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationSummaryDto {
    private Long id;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String status;
    private LocalDateTime appliedAt;
}

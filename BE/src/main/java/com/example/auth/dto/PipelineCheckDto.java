package com.example.auth.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineCheckDto {
    private Long jobRequestId;
    private String jobTitle;
    private String status;
    private int totalApplications;
    private int activeApplicationsCount;
    private boolean hasActiveCandidates;
    private String warningMessage;
    private List<ApplicationSummaryDto> activeApplications;
}

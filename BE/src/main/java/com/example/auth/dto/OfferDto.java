package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDto {
    private Long id;
    private String candidateName;
    private String jobTitle;
    private String positionCode;
    private String positionLevel;
    private Double offeredSalary;
    private Double maxSalary;
    private Double maxSalaryAllowed;
    private Boolean exceedsLimit;
    private String status;
    private String note;
    private String approvalNote;
}

package com.example.auth.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRuleStepDto {
    private Long id;
    private Integer stepOrder;
    private String stepName;
    private String approverRole;
    private Long approverUserId;
    private String approverUserName;
    private String approverUserFullName;
    private String description;
}

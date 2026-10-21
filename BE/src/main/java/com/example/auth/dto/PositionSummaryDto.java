package com.example.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionSummaryDto {
    private Long id;
    private String code;
    private String name;
    private String level;
    private String departmentName;
}

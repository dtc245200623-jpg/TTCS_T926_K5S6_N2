package com.example.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentManagerDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String title;
    private String department;
}

package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserImportRowDto {
    private int rowNumber;
    private String username;
    private String email;
    private String fullName;
    private String department;
    
    @Builder.Default
    private Set<String> roles = new HashSet<>();
    
    private boolean valid;
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}

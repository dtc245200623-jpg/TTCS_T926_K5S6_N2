package com.example.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private boolean isLocked;
    private java.time.LocalDateTime createdAt;
    private String fullName;
    private String department;
    private String phoneNumber;
    private String title;
    private String avatarUrl;
}

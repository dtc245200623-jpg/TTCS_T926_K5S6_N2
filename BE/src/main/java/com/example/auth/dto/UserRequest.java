package com.example.auth.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserRequest {
    private String email;
    private String password;
    private Set<String> roles;
    private String username;
    private String fullName;
    private String department;
    private String phoneNumber;
    private String title;
}

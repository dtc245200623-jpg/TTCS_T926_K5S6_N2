package com.example.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String username;
    private java.util.Set<String> roles;
    private Long tokenVersion;
    private String refreshToken;
}

package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePasswordResponse {

    private String message;
    private Long newTokenVersion;
    private String newAccessToken; // Token mới cho phiên hiện tại (nếu cấu hình cấp lại ngay)
}

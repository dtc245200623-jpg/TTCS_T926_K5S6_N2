package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * KN-307: DTO ghi chú cho hồ sơ ứng viên
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationNoteDto {
    private Long id;
    private Long applicationId;
    private String content;
    private String authorUsername;
    private String authorFullName;
    private LocalDateTime createdAt;
}

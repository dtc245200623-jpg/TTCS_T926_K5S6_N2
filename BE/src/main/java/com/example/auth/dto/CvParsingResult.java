package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả bóc tách dữ liệu từ file CV (KN-286, KN-289, KN-290)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvParsingResult {
    private String status; // SUCCESS, FAILED, SKIPPED
    private String fullName;
    private String email;
    private String phone;
    private String rawTextSnippet;
    private String errorMessage;

    public static CvParsingResult success(String fullName, String email, String phone, String rawTextSnippet) {
        return CvParsingResult.builder()
                .status("SUCCESS")
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .rawTextSnippet(rawTextSnippet)
                .build();
    }

    public static CvParsingResult failed(String errorMessage) {
        return CvParsingResult.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }

    public static CvParsingResult skipped(String note) {
        return CvParsingResult.builder()
                .status("SKIPPED")
                .errorMessage(note)
                .build();
    }
}

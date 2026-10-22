package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * KN-304, KN-305, KN-306, KN-307, KN-308:
 * DTO chi tiết toàn diện của hồ sơ ứng viên dành cho Recruiter & Hiring Manager
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDetailDto {
    private Long id;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String address;
    private String currentTitle;
    private String coverLetter;
    private String cvUrl;
    private String cvOriginalFilename;
    private String trackingCode;
    private String status;
    private String statusDisplayName;
    private LocalDateTime appliedAt;
    
    // Nguồn ứng tuyển
    private Long sourceId;
    private String sourceName;
    
    // Vị trí ứng tuyển
    private Long jobRequestId;
    private String jobTitle;
    private Long departmentId;
    private String departmentName;
    
    // Người phụ trách
    private String recruiterUsername;
    private String recruiterFullName;
    private String recruiterEmail;
    private String hiringManagerUsername;
    private String hiringManagerFullName;
    private String hiringManagerEmail;
    
    // KN-307: Nhãn và Ghi chú
    private List<String> tags;
    private List<ApplicationNoteDto> notes;
    
    // KN-306: Toàn bộ lịch sử các lần ứng tuyển trước đây của cùng người này
    private List<CandidateHistoryDto> history;
    
    // Dữ liệu trích xuất từ CV
    private String parsedFullName;
    private String parsedEmail;
    private String parsedPhone;
    private String cvParsingStatus;
    private String cvParsingNote;
    private Boolean parsedDataConfirmed;
    
    // KN-295: Rút hồ sơ
    private String withdrawalReason;
    private LocalDateTime withdrawnAt;
    
    // Quyền của người xem hiện tại
    private Boolean canEditNotes;
    private Boolean canUpdateStatus;
}

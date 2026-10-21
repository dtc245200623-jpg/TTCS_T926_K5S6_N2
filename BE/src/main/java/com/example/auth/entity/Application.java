package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne @JoinColumn(name = "candidate_profile_id")
    private CandidateProfile candidateProfile;
    
    @ManyToOne @JoinColumn(name = "job_request_id")
    private JobRequest jobRequest;
    
    @Column(length = 50)
    private String status; // APPLIED, SCREENING, INTERVIEWING, OFFERING, HIRED, REJECTED
    
    // KN-188: Nguồn ứng viên tham chiếu tới danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private RecruitmentCategory source;

    // KN-189: Lý do loại hồ sơ tham chiếu tới danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejection_reason_id")
    private RecruitmentCategory rejectionReason;

    @Column(name = "rejection_note", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionNote;

    @Column(name = "applied_at")
    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();

    // KN-284, KN-291: Mã tra cứu duy nhất cho ứng viên
    @Column(name = "tracking_code", length = 50, unique = true)
    private String trackingCode;

    // KN-279, KN-285: Thông tin ứng viên tự nhập trực tiếp từ biểu mẫu
    @Column(name = "candidate_name", columnDefinition = "NVARCHAR(255)")
    private String candidateName;

    @Column(name = "candidate_email", length = 150)
    private String candidateEmail;

    @Column(name = "candidate_phone", length = 30)
    private String candidatePhone;

    @Column(name = "cover_letter", columnDefinition = "NVARCHAR(MAX)")
    private String coverLetter;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;

    @Column(name = "cv_original_filename", length = 255)
    private String cvOriginalFilename;

    // KN-282: Địa chỉ IP nộp hồ sơ để giới hạn tần suất chống spam
    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    // KN-286, KN-287, KN-288, KN-289: Dữ liệu bóc tách từ tệp CV PDF (lưu riêng, không tự ghi đè)
    @Column(name = "parsed_full_name", columnDefinition = "NVARCHAR(255)")
    private String parsedFullName;

    @Column(name = "parsed_email", length = 150)
    private String parsedEmail;

    @Column(name = "parsed_phone", length = 30)
    private String parsedPhone;

    @Column(name = "cv_parsing_status", length = 50)
    @Builder.Default
    private String cvParsingStatus = "NONE"; // NONE, SUCCESS, FAILED, SKIPPED

    @Column(name = "cv_parsing_note", columnDefinition = "NVARCHAR(500)")
    private String cvParsingNote;

    @Column(name = "parsed_data_confirmed")
    @Builder.Default
    private Boolean parsedDataConfirmed = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (appliedAt == null) appliedAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

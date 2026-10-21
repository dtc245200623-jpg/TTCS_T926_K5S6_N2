package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_profiles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne @JoinColumn(name = "user_id")
    private User user;
    
    @Column(columnDefinition = "NVARCHAR(255)")
    private String fullName;
    private String phone;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String address;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String currentTitle;
    private String cvUrl;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

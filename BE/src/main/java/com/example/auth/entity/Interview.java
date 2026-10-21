package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Interview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne @JoinColumn(name = "application_id")
    private Application application;
    
    @ManyToOne @JoinColumn(name = "interviewer_id")
    private User interviewer;
    
    private LocalDateTime scheduledAt;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String location;
    private String meetingLink;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String feedback;
    
    @Column(length = 50)
    private String status; // SCHEDULED, COMPLETED, CANCELLED
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

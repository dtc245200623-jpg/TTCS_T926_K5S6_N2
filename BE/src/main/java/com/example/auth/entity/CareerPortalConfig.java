package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "career_portal_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerPortalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String companyName;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String tagline;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String introduction;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String aboutCulture;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String benefits;

    @Column(columnDefinition = "VARCHAR(500)")
    private String logoUrl;

    @Column(columnDefinition = "VARCHAR(500)")
    private String bannerUrl;

    @Column(columnDefinition = "VARCHAR(255)")
    private String email;

    @Column(columnDefinition = "VARCHAR(50)")
    private String phone;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String address;

    @Column(columnDefinition = "VARCHAR(255)")
    private String website;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String workingHours;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", columnDefinition = "NVARCHAR(100)")
    private String updatedBy;
}

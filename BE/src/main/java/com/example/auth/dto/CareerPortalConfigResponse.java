package com.example.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerPortalConfigResponse {
    private Long id;
    private String companyName;
    private String tagline;
    private String introduction;
    private String aboutCulture;
    private String benefits;
    private String logoUrl;
    private String bannerUrl;
    private String email;
    private String phone;
    private String address;
    private String website;
    private String workingHours;
    private Boolean isPublished;
    private LocalDateTime updatedAt;
    private String updatedBy;
}

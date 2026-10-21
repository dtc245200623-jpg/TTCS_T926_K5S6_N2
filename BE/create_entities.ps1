$entitiesDir = "c:\Users\Acer\Downloads\github\TTCS_T926_K5S6_N2\src\main\java\com\example\auth\entity"

# 1. Department
$deptCode = @"
package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\Department.java" -Value $deptCode -Encoding UTF8

# 2. JobRequest
$jobReqCode = @"
package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_requests")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class JobRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    
    @ManyToOne @JoinColumn(name = "department_id")
    private Department department;
    
    @ManyToOne @JoinColumn(name = "hiring_manager_id")
    private User hiringManager;
    
    @ManyToOne @JoinColumn(name = "recruiter_id")
    private User recruiter;
    
    private Integer headcount;
    private Double minSalary;
    private Double maxSalary;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String requirements;
    
    @Column(length = 50)
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, OPEN, CLOSED
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\JobRequest.java" -Value $jobReqCode -Encoding UTF8

# 3. CandidateProfile
$profileCode = @"
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
    
    private String fullName;
    private String phone;
    private String address;
    private String currentTitle;
    private String cvUrl;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\CandidateProfile.java" -Value $profileCode -Encoding UTF8

# 4. Application
$appCode = @"
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
    
    @Column(name = "applied_at")
    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\Application.java" -Value $appCode -Encoding UTF8

# 5. Interview
$interviewCode = @"
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
    
    private LocalDateTime scheduledTime;
    private String location;
    private String meetingLink;
    
    @Column(length = 50)
    private String status; // SCHEDULED, COMPLETED, CANCELLED
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\Interview.java" -Value $interviewCode -Encoding UTF8

# 6. Evaluation
$evalCode = @"
package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne @JoinColumn(name = "interview_id")
    private Interview interview;
    
    private Integer score; // 1 to 10
    
    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    @Column(length = 50)
    private String recommendedAction; // HIRE, REJECT, NEXT_ROUND
    
    @Column(name = "evaluated_at")
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\Evaluation.java" -Value $evalCode -Encoding UTF8

# 7. Offer
$offerCode = @"
package com.example.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Offer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne @JoinColumn(name = "application_id")
    private Application application;
    
    private Double offeredSalary;
    private LocalDateTime expectedStartDate;
    
    @Column(columnDefinition = "TEXT")
    private String note;
    
    @Column(length = 50)
    private String status; // PENDING_APPROVAL, APPROVED, SENT, ACCEPTED, DECLINED
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
"@
Set-Content -Path "$entitiesDir\Offer.java" -Value $offerCode -Encoding UTF8

package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.InterviewDto;
import com.example.auth.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {
    private final InterviewService interviewService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'HR_MANAGER', 'INTERVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getAllInterviews() {
        List<InterviewDto> list = interviewService.getAllInterviews();
        return ResponseEntity.ok(ApiResponse.success("Danh sách lịch phỏng vấn", list));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InterviewDto>> scheduleInterview(@RequestBody Map<String, Object> request) {
        Long appId = Long.valueOf(request.get("applicationId").toString());
        String interviewer = request.get("interviewerUsername").toString();
        LocalDateTime time = LocalDateTime.parse(request.get("scheduledAt").toString());
        
        InterviewDto dto = interviewService.scheduleInterview(appId, interviewer, time);
        return ResponseEntity.ok(ApiResponse.success("Đặt lịch thành công", dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getMyInterviews(Authentication authentication) {
        String username = authentication.getName();
        List<InterviewDto> list = interviewService.getInterviewsByInterviewer(username);
        return ResponseEntity.ok(ApiResponse.success("Lịch phỏng vấn của tôi", list));
    }

    @GetMapping("/candidate")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getCandidateInterviews(Authentication authentication) {
        String username = authentication.getName();
        List<InterviewDto> list = interviewService.getInterviewsByCandidate(username);
        return ResponseEntity.ok(ApiResponse.success("Lịch phỏng vấn của tôi", list));
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InterviewDto>> submitFeedback(
            @PathVariable Long id, 
            @RequestBody Map<String, String> body) {
        InterviewDto dto = interviewService.submitFeedback(id, body.get("feedback"));
        return ResponseEntity.ok(ApiResponse.success("Nộp đánh giá thành công", dto));
    }
}

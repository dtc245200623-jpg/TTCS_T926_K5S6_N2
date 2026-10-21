package com.example.auth.service;

import com.example.auth.dto.InterviewDto;
import java.time.LocalDateTime;
import java.util.List;

public interface InterviewService {
    InterviewDto scheduleInterview(Long applicationId, String interviewerUsername, LocalDateTime scheduledAt);
    List<InterviewDto> getInterviewsByInterviewer(String username);
    List<InterviewDto> getInterviewsByCandidate(String username);
    List<InterviewDto> getAllInterviews();
    InterviewDto submitFeedback(Long interviewId, String feedback);
}

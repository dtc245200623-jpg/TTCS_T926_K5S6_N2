package com.example.auth.service.impl;

import com.example.auth.dto.InterviewDto;
import com.example.auth.entity.Application;
import com.example.auth.entity.Interview;
import com.example.auth.entity.User;
import com.example.auth.repository.ApplicationRepository;
import com.example.auth.repository.InterviewRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {
    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Override
    public InterviewDto scheduleInterview(Long applicationId, String interviewerUsername, LocalDateTime scheduledAt) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("App not found"));
        User interviewer = userRepository.findByUsername(interviewerUsername)
                .orElseThrow(() -> new RuntimeException("Interviewer not found"));

        Interview interview = new Interview();
        interview.setApplication(app);
        interview.setInterviewer(interviewer);
        interview.setScheduledAt(scheduledAt);
        interview.setStatus("SCHEDULED");
        
        Interview saved = interviewRepository.save(interview);
        
        app.setStatus("INTERVIEWING");
        applicationRepository.save(app);
        
        return mapToDto(saved);
    }

    @Override
    public List<InterviewDto> getInterviewsByInterviewer(String username) {
        User interviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return interviewRepository.findAll().stream()
                .filter(i -> i.getInterviewer().getId().equals(interviewer.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDto> getInterviewsByCandidate(String username) {
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return interviewRepository.findAll().stream()
                .filter(i -> i.getApplication().getCandidateProfile() != null && i.getApplication().getCandidateProfile().getUser().getId().equals(candidate.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDto> getAllInterviews() {
        return interviewRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public InterviewDto submitFeedback(Long interviewId, String feedback) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
        interview.setFeedback(feedback);
        interview.setStatus("COMPLETED");
        Interview saved = interviewRepository.save(interview);
        return mapToDto(saved);
    }

    private InterviewDto mapToDto(Interview i) {
        return InterviewDto.builder()
                .id(i.getId())
                .applicationId(i.getApplication().getId())
                .candidateName(i.getApplication().getCandidateProfile() != null ? i.getApplication().getCandidateProfile().getUser().getUsername() : "Unknown")
                .jobTitle(i.getApplication().getJobRequest().getTitle())
                .interviewerName(i.getInterviewer().getUsername())
                .scheduledAt(i.getScheduledAt())
                .status(i.getStatus())
                .feedback(i.getFeedback())
                .location(i.getLocation())
                .meetingLink(i.getMeetingLink())
                .build();
    }
}

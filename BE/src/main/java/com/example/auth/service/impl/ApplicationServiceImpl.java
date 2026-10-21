package com.example.auth.service.impl;

import com.example.auth.dto.ApplicationDto;
import com.example.auth.entity.Application;
import com.example.auth.entity.CandidateProfile;
import com.example.auth.entity.JobRequest;
import com.example.auth.entity.User;
import com.example.auth.repository.ApplicationRepository;
import com.example.auth.repository.CandidateProfileRepository;
import com.example.auth.repository.JobRequestRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRequestRepository jobRequestRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    @Override
    public ApplicationDto applyForJob(Long jobRequestId, String username, String cvUrl) {
        JobRequest job = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy Vị trí tuyển dụng với ID này"));
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy thông tin ứng viên"));
        
        CandidateProfile profile = candidateProfileRepository.findByUserId(candidate.getId())
                .orElseGet(() -> {
                    CandidateProfile newProfile = new CandidateProfile();
                    newProfile.setUser(candidate);
                    newProfile.setFullName(candidate.getUsername());
                    return candidateProfileRepository.save(newProfile);
                });
        
        if (cvUrl != null) {
            profile.setCvUrl(cvUrl);
            candidateProfileRepository.save(profile);
        }

        Application app = new Application();
        app.setJobRequest(job);
        app.setCandidateProfile(profile);
        app.setStatus("APPLIED"); // APPLIED, SCREENING, INTERVIEWING, OFFERED, REJECTED
        app.setAppliedAt(LocalDateTime.now());
        
        Application saved = applicationRepository.save(app);
        return mapToDto(saved);
    }

    @Override
    public List<ApplicationDto> getApplicationsByCandidate(String username) {
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy thông tin ứng viên"));
        return applicationRepository.findAll().stream()
                .filter(a -> a.getCandidateProfile() != null && a.getCandidateProfile().getUser().getId().equals(candidate.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getApplicationsByJobRequest(Long jobRequestId) {
        return applicationRepository.findAll().stream()
                .filter(a -> a.getJobRequest().getId().equals(jobRequestId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationDto updateStatus(Long id, String status) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy hồ sơ ứng tuyển"));
        app.setStatus(status);
        Application saved = applicationRepository.save(app);
        return mapToDto(saved);
    }

    private ApplicationDto mapToDto(Application a) {
        String name = a.getCandidateName();
        if (name == null && a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null) {
            name = a.getCandidateProfile().getUser().getFullName() != null ? a.getCandidateProfile().getUser().getFullName() : a.getCandidateProfile().getUser().getUsername();
        }

        return ApplicationDto.builder()
                .id(a.getId())
                .candidateId(a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null ? a.getCandidateProfile().getUser().getId() : null)
                .candidateName(name != null ? name : "Ứng viên")
                .candidateEmail(a.getCandidateEmail() != null ? a.getCandidateEmail() : (a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null ? a.getCandidateProfile().getUser().getEmail() : null))
                .candidatePhone(a.getCandidatePhone() != null ? a.getCandidatePhone() : (a.getCandidateProfile() != null ? a.getCandidateProfile().getPhone() : null))
                .jobRequestId(a.getJobRequest() != null ? a.getJobRequest().getId() : null)
                .jobTitle(a.getJobRequest() != null ? a.getJobRequest().getTitle() : null)
                .status(a.getStatus())
                .appliedAt(a.getAppliedAt())
                .interviewScore("85/100") // Mock data cho phỏng vấn
                .cvUrl(a.getCvUrl() != null ? a.getCvUrl() : (a.getCandidateProfile() != null ? a.getCandidateProfile().getCvUrl() : null))
                .cvOriginalFilename(a.getCvOriginalFilename())
                .trackingCode(a.getTrackingCode())
                .coverLetter(a.getCoverLetter())
                .sourceId(a.getSource() != null ? a.getSource().getId() : null)
                .sourceName(a.getSource() != null ? a.getSource().getName() : null)
                .parsedFullName(a.getParsedFullName())
                .parsedEmail(a.getParsedEmail())
                .parsedPhone(a.getParsedPhone())
                .cvParsingStatus(a.getCvParsingStatus())
                .cvParsingNote(a.getCvParsingNote())
                .parsedDataConfirmed(a.getParsedDataConfirmed())
                .build();
    }
}

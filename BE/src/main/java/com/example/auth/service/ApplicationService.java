package com.example.auth.service;

import com.example.auth.dto.ApplicationDto;
import java.util.List;

public interface ApplicationService {
    ApplicationDto applyForJob(Long jobRequestId, String username, String cvUrl);
    List<ApplicationDto> getApplicationsByCandidate(String username);
    List<ApplicationDto> getApplicationsByJobRequest(Long jobRequestId);
    List<ApplicationDto> getAllApplications();
    ApplicationDto updateStatus(Long id, String status);
}

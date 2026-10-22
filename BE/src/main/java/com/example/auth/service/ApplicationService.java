package com.example.auth.service;

import com.example.auth.dto.ApplicationDto;
import java.util.List;

public interface ApplicationService {
    ApplicationDto applyForJob(Long jobRequestId, String username, String cvUrl);
    List<ApplicationDto> getApplicationsByCandidate(String username);
    List<ApplicationDto> getApplicationsByJobRequest(Long jobRequestId);
    List<ApplicationDto> getAllApplications();
    ApplicationDto updateStatus(Long id, String status);

    // KN-304, KN-306, KN-308: Lấy chi tiết hồ sơ ứng viên với phân quyền phụ trách và lịch sử ứng tuyển
    com.example.auth.dto.CandidateDetailDto getCandidateDetail(Long applicationId, String currentUsername);

    // KN-307: Quản lý ghi chú cho hồ sơ ứng viên
    List<com.example.auth.dto.ApplicationNoteDto> getApplicationNotes(Long applicationId, String currentUsername);
    com.example.auth.dto.ApplicationNoteDto addApplicationNote(Long applicationId, String content, String currentUsername);
    void deleteApplicationNote(Long noteId, String currentUsername);

    // KN-307: Quản lý nhãn (Tags) cho hồ sơ ứng viên
    com.example.auth.dto.CandidateDetailDto updateApplicationTags(Long applicationId, List<String> tags, String currentUsername);
}

package com.example.auth.service;

import com.example.auth.dto.CloseJobRequestDto;
import com.example.auth.dto.JobRequestDto;
import com.example.auth.dto.PipelineCheckDto;
import java.util.List;

public interface JobRequestService {
    JobRequestDto createOrDraftJobRequest(JobRequestDto dto, String username, boolean isDraft);
    JobRequestDto updateJobRequest(Long id, JobRequestDto dto, String username, boolean isDraft);
    JobRequestDto submitJobRequest(Long id, String username);
    void deleteJobRequest(Long id, String username);
    JobRequestDto getJobRequestById(Long id);
    List<JobRequestDto> getMyJobRequests(String username);
    List<JobRequestDto> getAllJobRequests();
    List<JobRequestDto> getJobRequestsByDepartment(Long departmentId);
    JobRequestDto approveOrReject(Long id, boolean isApproved, String approverUsername);
    JobRequestDto assignRecruiter(Long id, String recruiterUsername);
    JobRequestDto createJobRequest(JobRequestDto dto, String username);
    JobRequestDto cloneJobRequest(Long id, String username);

    // KN-246, KN-247, KN-248, KN-249: Kết thúc yêu cầu tuyển dụng và kiểm tra pipeline ứng viên
    PipelineCheckDto checkCandidatePipeline(Long jobRequestId);
    JobRequestDto closeJobRequest(Long jobRequestId, CloseJobRequestDto dto, String username);

    // KN-250: Lấy danh sách Recruiter phục vụ bộ lọc danh sách yêu cầu tuyển dụng
    java.util.List<java.util.Map<String, Object>> getAllRecruiters();

    // Đăng tin & Cổng ứng tuyển - Nhân viên tuyển dụng
    JobRequestDto publishJobPosting(Long id, com.example.auth.dto.PublishJobPostingDto dto, String username);
}

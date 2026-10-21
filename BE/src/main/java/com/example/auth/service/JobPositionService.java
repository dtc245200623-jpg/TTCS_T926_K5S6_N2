package com.example.auth.service;

import com.example.auth.dto.JobPositionRequest;
import com.example.auth.dto.JobPositionResponse;

import java.util.List;

public interface JobPositionService {

    // KN-177 & KN-180: Danh mục chức danh và kiểm soát hiển thị dải lương
    List<JobPositionResponse> getAllPositions(Boolean activeOnly, String search);

    JobPositionResponse getPositionById(Long id);

    // KN-178: Quản lý chức danh & dải lương
    JobPositionResponse createPosition(JobPositionRequest request);

    JobPositionResponse updatePosition(Long id, JobPositionRequest request);

    JobPositionResponse toggleStatus(Long id, Boolean active);

    void deletePosition(Long id);

    // KN-180: Kiểm tra quyền xem dải lương (ROLE_HR_MANAGER hoặc ROLE_ADMIN)
    boolean canCurrentUserViewSalary();
}

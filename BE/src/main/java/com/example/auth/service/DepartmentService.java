package com.example.auth.service;

import com.example.auth.dto.DepartmentManagerDto;
import com.example.auth.dto.DepartmentRequest;
import com.example.auth.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    // KN-174: Cấu trúc cây nhiều cấp
    List<DepartmentResponse> getDepartmentTree(Boolean activeOnly);

    List<DepartmentResponse> getAllDepartments(Boolean activeOnly, String search);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    // KN-175: Gán người phụ trách
    DepartmentResponse assignManager(Long departmentId, Long managerId);

    List<DepartmentManagerDto> getEligibleManagers();

    // KN-176: Bật tắt trạng thái áp dụng & Quy tắc không xoá khi có yêu cầu tuyển dụng mở
    DepartmentResponse toggleStatus(Long id, Boolean active);

    void deleteDepartment(Long id);
}

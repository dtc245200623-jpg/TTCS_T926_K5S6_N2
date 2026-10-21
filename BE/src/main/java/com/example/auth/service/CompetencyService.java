package com.example.auth.service;

import com.example.auth.dto.*;

import java.util.List;

public interface CompetencyService {

    // KN-182: Quản lý Khung năng lực dùng chung cho nhiều chức danh
    List<CompetencyFrameworkResponse> getAllFrameworks(Boolean activeOnly, String search);

    CompetencyFrameworkResponse getFrameworkById(Long id);

    // KN-183: Tạo / Cập nhật khung năng lực kèm tiêu chí (Validation: Tổng trọng số = 100%)
    CompetencyFrameworkResponse createFramework(CompetencyFrameworkRequest request);

    CompetencyFrameworkResponse updateFramework(Long id, CompetencyFrameworkRequest request);

    CompetencyFrameworkResponse toggleFrameworkStatus(Long id, Boolean active);

    void deleteFramework(Long id);

    // KN-184: Ánh xạ khung năng lực vào chức danh
    CompetencyFrameworkResponse assignPositions(Long frameworkId, List<Long> positionIds);

    void unassignPosition(Long positionId);

    // KN-184: Sinh phiếu đánh giá phỏng vấn từ khung năng lực của chức danh
    EvaluationTemplateResponse generateEvaluationTemplateForPosition(Long positionId);

    EvaluationTemplateResponse generateEvaluationTemplateForInterview(Long interviewId);

    // Lưu kết quả đánh giá phỏng vấn kèm điểm chi tiết từng tiêu chí và điểm tổng trọng số
    EvaluationDetailResponse submitEvaluation(EvaluationSubmitRequest request, String currentUsername);

    EvaluationDetailResponse getEvaluationByInterviewId(Long interviewId);
}
